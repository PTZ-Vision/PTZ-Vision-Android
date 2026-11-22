// NDI PTZ camera emulator for PTZ Vision testing.
//
// Publishes an NDI source that advertises PTZ support (<ndi_capabilities ntk_ptz="true"/>),
// renders a synthetic "camera view" whose framing follows the virtual PTZ state and logs
// every PTZ command it receives. Useful to test PTZ Vision without a real NDI camera.
//
// Build (on Linux with the NDI SDK installed):  make NDI_SDK_DIR="/path/to/NDI SDK for Linux"
// Run:  ./ndi_ptz_emulator ["Source name"] [width] [height]

#include <Processing.NDI.Lib.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

static std::atomic<bool> g_exit(false);
static void on_signal(int) { g_exit = true; }

// --- Minimal XML helpers (NDI PTZ messages are single, flat elements) ------------------------

static std::string xml_tag(const char* xml) {
    const char* p = std::strchr(xml, '<');
    if (!p) return {};
    ++p;
    const char* e = p;
    while (*e && *e != ' ' && *e != '/' && *e != '>' && *e != '\t' && *e != '\n') ++e;
    return std::string(p, e);
}

static bool xml_attr(const char* xml, const char* name, std::string& out) {
    const std::string needle = std::string(" ") + name + "=";
    const char* p = std::strstr(xml, needle.c_str());
    if (!p) return false;
    p += needle.size();
    const char quote = *p;
    if (quote != '"' && quote != '\'') return false;
    const char* e = std::strchr(p + 1, quote);
    if (!e) return false;
    out.assign(p + 1, e);
    return true;
}

static float attr_f(const char* xml, const char* name, float def) {
    std::string v;
    return xml_attr(xml, name, v) ? std::strtof(v.c_str(), nullptr) : def;
}

static int attr_i(const char* xml, const char* name, int def) {
    std::string v;
    return xml_attr(xml, name, v) ? std::atoi(v.c_str()) : def;
}

static float clampf(float v, float lo, float hi) { return std::max(lo, std::min(hi, v)); }

// --- Virtual PTZ state ------------------------------------------------------------------------

struct PtzState {
    float pan = 0.f, tilt = 0.f;        // -1..1 (absolute)
    float zoom = 0.f;                   // 0 (wide) .. 1 (tele) - internal convention
    float focus = 0.5f;
    bool auto_focus = true;
    float pan_speed = 0.f, tilt_speed = 0.f, zoom_speed = 0.f, focus_speed = 0.f;
    struct Preset { bool set = false; float pan = 0, tilt = 0, zoom = 0; } presets[100];
    int last_preset = -1;
    int flash = 0;                      // frames to highlight after a preset action
    unsigned long commands = 0;
};

static void handle_command(PtzState& s, const char* xml) {
    const std::string tag = xml_tag(xml);
    s.commands++;
    std::printf("[cmd #%lu] %s\n", s.commands, xml);

    if (tag == "ntk_ptz_pan_tilt_speed") {
        // NDI convention: pan_speed +1 = moving left, tilt_speed +1 = moving up.
        s.pan_speed = clampf(attr_f(xml, "pan_speed", 0.f), -1.f, 1.f);
        s.tilt_speed = clampf(attr_f(xml, "tilt_speed", 0.f), -1.f, 1.f);
    } else if (tag == "ntk_ptz_pan_tilt") {
        s.pan = clampf(attr_f(xml, "pan", 0.f), -1.f, 1.f);
        s.tilt = clampf(attr_f(xml, "tilt", 0.f), -1.f, 1.f);
        s.pan_speed = s.tilt_speed = 0.f;
    } else if (tag == "ntk_ptz_zoom_speed") {
        // NDI convention: +1 = zoom inwards (tele).
        s.zoom_speed = clampf(attr_f(xml, "zoom_speed", 0.f), -1.f, 1.f);
    } else if (tag == "ntk_ptz_zoom") {
        // NDI convention: 0 = zoomed in, 1 = zoomed out.
        s.zoom = 1.f - clampf(attr_f(xml, "zoom", 1.f), 0.f, 1.f);
        s.zoom_speed = 0.f;
    } else if (tag == "ntk_ptz_focus") {
        std::string mode;
        xml_attr(xml, "mode", mode);
        s.auto_focus = (mode != "manual");
        if (!s.auto_focus) s.focus = clampf(attr_f(xml, "distance", s.focus), 0.f, 1.f);
    } else if (tag == "ntk_ptz_focus_speed") {
        s.auto_focus = false;
        // The SDK sends the speed in the "distance" attribute.
        s.focus_speed = clampf(attr_f(xml, "distance", 0.f), -1.f, 1.f);
    } else if (tag == "ntk_ptz_store_preset") {
        const int i = attr_i(xml, "index", -1);
        if (i >= 0 && i < 100) {
            s.presets[i] = {true, s.pan, s.tilt, s.zoom};
            s.last_preset = i;
            s.flash = 20;
            std::printf("  -> stored preset %d (pan=%.2f tilt=%.2f zoom=%.2f)\n", i, s.pan, s.tilt, s.zoom);
        }
    } else if (tag == "ntk_ptz_recall_preset") {
        const int i = attr_i(xml, "index", -1);
        if (i >= 0 && i < 100) {
            if (s.presets[i].set) {
                s.pan = s.presets[i].pan;
                s.tilt = s.presets[i].tilt;
                s.zoom = s.presets[i].zoom;
            } else {
                s.pan = s.tilt = s.zoom = 0.f; // unknown preset -> home
            }
            s.pan_speed = s.tilt_speed = s.zoom_speed = 0.f;
            s.last_preset = i;
            s.flash = 20;
        }
    }
}

static void step(PtzState& s, float dt) {
    s.pan = clampf(s.pan - s.pan_speed * 0.5f * dt, -1.f, 1.f);   // + speed = left
    s.tilt = clampf(s.tilt + s.tilt_speed * 0.5f * dt, -1.f, 1.f);
    s.zoom = clampf(s.zoom + s.zoom_speed * 0.4f * dt, 0.f, 1.f);
    s.focus = clampf(s.focus + s.focus_speed * 0.4f * dt, 0.f, 1.f);
    if (s.flash > 0) s.flash--;
}

// --- Rendering (BGRX) -------------------------------------------------------------------------

static void render(const PtzState& s, std::vector<uint8_t>& buf, int w, int h, int frame) {
    // Map the virtual "world" (a big checker/grid scene) through the PTZ transform.
    const float scale = 1.f + s.zoom * 9.f;          // 1x .. 10x
    const float cx = s.pan * 4.f, cy = -s.tilt * 2.f;  // world coordinates of the view centre
    const float inv = 1.f / (scale * h);
    // Defocus visualisation: in manual mode, blur grows with distance from "correct" focus 0.5.
    const float blur = s.auto_focus ? 0.f : std::fabs(s.focus - 0.5f) * 2.f;

    for (int y = 0; y < h; ++y) {
        uint8_t* row = &buf[(size_t)y * w * 4];
        const float wy = cy + (y - h * 0.5f) * inv * 2.f;
        for (int x = 0; x < w; ++x) {
            const float wx = cx + (x - w * 0.5f) * inv * 2.f;
            const int gx = (int)std::floor(wx * 4.f), gy = (int)std::floor(wy * 4.f);
            const bool checker = ((gx + gy) & 1) != 0;
            const float fx = wx * 4.f - std::floor(wx * 4.f), fy = wy * 4.f - std::floor(wy * 4.f);
            const float line = (fx < 0.04f || fy < 0.04f) ? 1.f : 0.f;
            uint8_t r = (uint8_t)(60 + ((gx * 37) & 127));
            uint8_t g = (uint8_t)(60 + ((gy * 53) & 127));
            uint8_t b = checker ? 200 : 90;
            if (line > 0.f) r = g = b = (uint8_t)(255 - blur * 160);
            row[x * 4 + 0] = b;
            row[x * 4 + 1] = g;
            row[x * 4 + 2] = r;
            row[x * 4 + 3] = 255;
        }
    }
    // Centre crosshair.
    for (int i = -20; i <= 20; ++i) {
        for (int t = -1; t <= 1; ++t) {
            const int x1 = w / 2 + i, y1 = h / 2 + t, x2 = w / 2 + t, y2 = h / 2 + i;
            if (x1 >= 0 && x1 < w && y1 >= 0 && y1 < h) std::memset(&buf[((size_t)y1 * w + x1) * 4], 255, 3);
            if (x2 >= 0 && x2 < w && y2 >= 0 && y2 < h) std::memset(&buf[((size_t)y2 * w + x2) * 4], 255, 3);
        }
    }
    // Bar indicators: zoom (bottom, yellow), focus (right side, cyan), preset flash border (green).
    const int zw = (int)(s.zoom * (w - 20));
    for (int y = h - 12; y < h - 4; ++y)
        for (int x = 10; x < 10 + zw; ++x) { uint8_t* p = &buf[((size_t)y * w + x) * 4]; p[0] = 0; p[1] = 220; p[2] = 255; }
    const int fh = (int)(s.focus * (h - 20));
    for (int y = h - 10 - fh; y < h - 10; ++y)
        for (int x = w - 12; x < w - 4; ++x) { uint8_t* p = &buf[((size_t)y * w + x) * 4]; p[0] = 255; p[1] = s.auto_focus ? 255 : 120; p[2] = 0; }
    if (s.flash > 0) {
        for (int y = 0; y < h; ++y)
            for (int x = 0; x < w; ++x)
                if (x < 6 || y < 6 || x >= w - 6 || y >= h - 6) { uint8_t* p = &buf[((size_t)y * w + x) * 4]; p[0] = 0; p[1] = 255; p[2] = 0; }
    }
    // Moving "heartbeat" pixel block to make frame delivery visible.
    const int hb = (frame * 4) % w;
    for (int y = 4; y < 10; ++y)
        for (int x = hb; x < std::min(w, hb + 6); ++x) { uint8_t* p = &buf[((size_t)y * w + x) * 4]; p[0] = p[1] = p[2] = 255; }
}

int main(int argc, char** argv) {
    const char* name = argc > 1 ? argv[1] : "PTZ Emulator";
    const int w = argc > 2 ? std::atoi(argv[2]) : 1280;
    const int h = argc > 3 ? std::atoi(argv[3]) : 720;

    if (!NDIlib_initialize()) {
        std::fprintf(stderr, "NDI cannot run on this machine (unsupported CPU?)\n");
        return 1;
    }
    std::signal(SIGINT, on_signal);
    std::signal(SIGTERM, on_signal);

    NDIlib_send_create_t desc;
    desc.p_ndi_name = name;
    desc.clock_video = true;
    NDIlib_send_instance_t send = NDIlib_send_create(&desc);
    if (!send) {
        std::fprintf(stderr, "Failed to create NDI sender\n");
        return 1;
    }

    // Advertise PTZ support: receivers' NDIlib_recv_ptz_is_supported() becomes true.
    NDIlib_metadata_frame_t caps;
    caps.p_data = const_cast<char*>("<ndi_capabilities ntk_ptz=\"true\" ntk_exposure_v2=\"true\"/>");
    NDIlib_send_add_connection_metadata(send, &caps);

    std::vector<uint8_t> buf((size_t)w * h * 4);
    NDIlib_video_frame_v2_t vf;
    vf.xres = w;
    vf.yres = h;
    vf.FourCC = NDIlib_FourCC_type_BGRX;
    vf.frame_rate_N = 30000;
    vf.frame_rate_D = 1001;
    vf.picture_aspect_ratio = (float)w / (float)h;
    vf.frame_format_type = NDIlib_frame_format_type_progressive;
    vf.line_stride_in_bytes = w * 4;

    std::printf("NDI PTZ emulator \"%s\" running at %dx%d (NDI %s). Ctrl+C to stop.\n",
                name, w, h, NDIlib_version());

    PtzState state;
    int frame = 0, last_conns = -1;
    auto last = std::chrono::steady_clock::now();
    while (!g_exit) {
        NDIlib_metadata_frame_t md;
        while (NDIlib_send_capture(send, &md, 0) == NDIlib_frame_type_metadata) {
            if (md.p_data) handle_command(state, md.p_data);
            NDIlib_send_free_metadata(send, &md);
        }

        const auto now = std::chrono::steady_clock::now();
        const float dt = std::chrono::duration<float>(now - last).count();
        last = now;
        step(state, dt);

        render(state, buf, w, h, frame++);
        vf.p_data = buf.data();
        NDIlib_send_send_video_v2(send, &vf); // clocked to ~29.97 fps

        const int conns = NDIlib_send_get_no_connections(send, 0);
        if (conns != last_conns) {
            std::printf("[info] receivers connected: %d\n", conns);
            last_conns = conns;
        }
        if (frame % 150 == 0)
            std::printf("[state] pan=%.2f tilt=%.2f zoom=%.2f focus=%.2f(%s) cmds=%lu\n", state.pan, state.tilt,
                        state.zoom, state.focus, state.auto_focus ? "auto" : "manual", state.commands);
        std::fflush(stdout);
    }

    NDIlib_send_destroy(send);
    NDIlib_destroy();
    return 0;
}
