// JNI bridge between PTZ Vision (Kotlin) and the NDI® SDK.
//
// The NDI runtime (libndi.so) is NOT linked at build time: it is loaded with dlopen() at runtime
// through NDIlib_v6_load(). This keeps the app buildable without the proprietary binaries; when
// libndi.so is not packaged, NdiNative.init() simply reports that NDI is unavailable.
//
// NDI® is a registered trademark of Vizrt NDI AB.

#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <algorithm>
#include <atomic>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "Processing.NDI.Lib.h"

#define LOG_TAG "NdiBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

const NDIlib_v6* g_ndi = nullptr;
std::mutex g_init_mutex;
std::string g_init_error;

struct Receiver {
    NDIlib_recv_instance_t recv = nullptr;
    std::thread thread;
    std::atomic<bool> running{false};

    std::mutex window_mutex;
    ANativeWindow* window = nullptr;
    int window_w = 0, window_h = 0;

    std::atomic<bool> ptz_supported{false};
    std::atomic<int> connections{0};
    std::atomic<int> width{0}, height{0};
    std::atomic<long long> frames{0};
};

void copy_frame_to_window(Receiver* r, const NDIlib_video_frame_v2_t& vf) {
    std::lock_guard<std::mutex> lock(r->window_mutex);
    ANativeWindow* win = r->window;
    if (!win) return;
    if (vf.FourCC != NDIlib_FourCC_type_RGBX && vf.FourCC != NDIlib_FourCC_type_RGBA) return;

    if (r->window_w != vf.xres || r->window_h != vf.yres) {
        ANativeWindow_setBuffersGeometry(win, vf.xres, vf.yres, AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM);
        r->window_w = vf.xres;
        r->window_h = vf.yres;
    }

    ANativeWindow_Buffer buf;
    if (ANativeWindow_lock(win, &buf, nullptr) != 0) return;

    const int rows = std::min(buf.height, vf.yres);
    const size_t row_bytes = (size_t)std::min(buf.width, vf.xres) * 4;
    const int src_stride = vf.line_stride_in_bytes > 0 ? vf.line_stride_in_bytes : vf.xres * 4;
    auto* dst = static_cast<uint8_t*>(buf.bits);
    const uint8_t* src = vf.p_data;
    for (int y = 0; y < rows; ++y) {
        std::memcpy(dst + (size_t)y * buf.stride * 4, src + (size_t)y * src_stride, row_bytes);
    }
    ANativeWindow_unlockAndPost(win);
}

void capture_loop(Receiver* r) {
    int tick = 0;
    while (r->running.load()) {
        bool want_video;
        {
            std::lock_guard<std::mutex> lock(r->window_mutex);
            want_video = r->window != nullptr;
        }

        NDIlib_video_frame_v2_t vf;
        NDIlib_metadata_frame_t md;
        // Passing a null video pointer makes the SDK drop video frames without decoding them.
        const NDIlib_frame_type_e type =
            g_ndi->recv_capture_v3(r->recv, want_video ? &vf : nullptr, nullptr, &md, 100);

        switch (type) {
            case NDIlib_frame_type_video:
                r->width = vf.xres;
                r->height = vf.yres;
                r->frames++;
                copy_frame_to_window(r, vf);
                g_ndi->recv_free_video_v2(r->recv, &vf);
                break;
            case NDIlib_frame_type_metadata:
                g_ndi->recv_free_metadata(r->recv, &md);
                break;
            case NDIlib_frame_type_status_change:
                r->ptz_supported = g_ndi->recv_ptz_is_supported(r->recv);
                break;
            case NDIlib_frame_type_error:
                LOGW("recv_capture reported an error (connection lost?)");
                break;
            default:
                break;
        }

        // Cheap periodic polling (every ~10 iterations).
        if (++tick % 10 == 0) {
            r->connections = g_ndi->recv_get_no_connections(r->recv);
            r->ptz_supported = g_ndi->recv_ptz_is_supported(r->recv);
        }
    }
}

Receiver* as_receiver(jlong h) { return reinterpret_cast<Receiver*>(h); }

std::string jstr(JNIEnv* env, jstring s) {
    if (!s) return {};
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

}  // namespace

extern "C" {

// Returns null on success, otherwise a human readable error.
JNIEXPORT jstring JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_nativeInit(JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(g_init_mutex);
    if (g_ndi) return nullptr;
    if (!g_init_error.empty()) return env->NewStringUTF(g_init_error.c_str());

    void* handle = dlopen("libndi.so", RTLD_NOW | RTLD_LOCAL);
    if (!handle) {
        const char* err = dlerror();
        g_init_error = std::string("NDI runtime (libndi.so) not found: ") + (err ? err : "unknown error");
        LOGW("%s", g_init_error.c_str());
        return env->NewStringUTF(g_init_error.c_str());
    }
    // Android packages lib*.so, but NDI opens the FFmpeg libraries by their versioned
    // SONAMEs. Preload in dependency order and retain them for the lifetime of the SDK.
    for (const char* library : {"libavutil.so", "libavcodec.so"}) {
        if (!dlopen(library, RTLD_NOW | RTLD_LOCAL)) {
            LOGW("NDI HX decoder library %s unavailable: %s", library, dlerror());
        }
    }
    using load_fn = const NDIlib_v6* (*)();
    auto load = reinterpret_cast<load_fn>(dlsym(handle, "NDIlib_v6_load"));
    if (!load) {
        g_init_error = "libndi.so does not export NDIlib_v6_load (runtime too old?)";
        LOGE("%s", g_init_error.c_str());
        return env->NewStringUTF(g_init_error.c_str());
    }
    const NDIlib_v6* lib = load();
    if (!lib || !lib->initialize()) {
        g_init_error = "NDIlib initialize() failed (unsupported CPU?)";
        LOGE("%s", g_init_error.c_str());
        return env->NewStringUTF(g_init_error.c_str());
    }
    g_ndi = lib;
    LOGI("NDI runtime loaded: %s", g_ndi->version());
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_nativeVersion(JNIEnv* env, jobject) {
    return g_ndi ? env->NewStringUTF(g_ndi->version()) : nullptr;
}

// ---------------------------------------------------------------------------------------------
// Finder
// ---------------------------------------------------------------------------------------------

JNIEXPORT jlong JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_finderCreate(JNIEnv* env, jobject, jstring extra_ips) {
    if (!g_ndi) return 0;
    const std::string ips = jstr(env, extra_ips);
    NDIlib_find_create_t desc;
    desc.show_local_sources = true;
    desc.p_groups = nullptr;
    desc.p_extra_ips = ips.empty() ? nullptr : ips.c_str();
    return reinterpret_cast<jlong>(g_ndi->find_create_v2(&desc));
}

// Returns [name0, url0, name1, url1, ...] once the source list changes or the timeout elapses.
JNIEXPORT jobjectArray JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_finderGetSources(JNIEnv* env, jobject, jlong finder, jint timeout_ms) {
    jclass string_cls = env->FindClass("java/lang/String");
    if (!g_ndi || !finder) return env->NewObjectArray(0, string_cls, nullptr);
    auto f = reinterpret_cast<NDIlib_find_instance_t>(finder);
    g_ndi->find_wait_for_sources(f, (uint32_t)timeout_ms);
    uint32_t n = 0;
    const NDIlib_source_t* src = g_ndi->find_get_current_sources(f, &n);
    jobjectArray out = env->NewObjectArray((jsize)(n * 2), string_cls, nullptr);
    for (uint32_t i = 0; i < n; ++i) {
        jstring name = env->NewStringUTF(src[i].p_ndi_name ? src[i].p_ndi_name : "");
        jstring url = env->NewStringUTF(src[i].p_url_address ? src[i].p_url_address : "");
        env->SetObjectArrayElement(out, (jsize)(i * 2), name);
        env->SetObjectArrayElement(out, (jsize)(i * 2 + 1), url);
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(url);
    }
    return out;
}

JNIEXPORT void JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_finderDestroy(JNIEnv*, jobject, jlong finder) {
    if (g_ndi && finder) g_ndi->find_destroy(reinterpret_cast<NDIlib_find_instance_t>(finder));
}

// ---------------------------------------------------------------------------------------------
// Receiver
// ---------------------------------------------------------------------------------------------

JNIEXPORT jlong JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_receiverCreate(
        JNIEnv* env, jobject, jstring source_name, jstring url_address, jboolean high_bandwidth, jstring recv_name) {
    if (!g_ndi) return 0;
    const std::string name = jstr(env, source_name);
    const std::string url = jstr(env, url_address);
    const std::string rname = jstr(env, recv_name);

    NDIlib_recv_create_v3_t desc;
    desc.source_to_connect_to.p_ndi_name = name.empty() ? nullptr : name.c_str();
    desc.source_to_connect_to.p_url_address = url.empty() ? nullptr : url.c_str();
    desc.color_format = NDIlib_recv_color_format_RGBX_RGBA;
    desc.bandwidth = high_bandwidth ? NDIlib_recv_bandwidth_highest : NDIlib_recv_bandwidth_lowest;
    desc.allow_video_fields = false;
    desc.p_ndi_recv_name = rname.empty() ? "PTZ Vision" : rname.c_str();

    NDIlib_recv_instance_t recv = g_ndi->recv_create_v3(&desc);
    if (!recv) {
        LOGE("recv_create_v3 failed for '%s' (%s)", name.c_str(), url.c_str());
        return 0;
    }
    auto* r = new Receiver();
    r->recv = recv;
    r->running = true;
    r->thread = std::thread(capture_loop, r);
    LOGI("Receiver created for '%s' (%s)", name.c_str(), url.c_str());
    return reinterpret_cast<jlong>(r);
}

JNIEXPORT void JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_receiverDestroy(JNIEnv*, jobject, jlong h) {
    Receiver* r = as_receiver(h);
    if (!r) return;
    r->running = false;
    if (r->thread.joinable()) r->thread.join();
    {
        std::lock_guard<std::mutex> lock(r->window_mutex);
        if (r->window) ANativeWindow_release(r->window);
        r->window = nullptr;
    }
    g_ndi->recv_destroy(r->recv);
    delete r;
}

JNIEXPORT void JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_receiverSetSurface(JNIEnv* env, jobject, jlong h, jobject surface) {
    Receiver* r = as_receiver(h);
    if (!r) return;
    ANativeWindow* next = surface ? ANativeWindow_fromSurface(env, surface) : nullptr;
    std::lock_guard<std::mutex> lock(r->window_mutex);
    if (r->window) ANativeWindow_release(r->window);
    r->window = next;
    r->window_w = r->window_h = 0;
}

// Returns [connections, ptzSupported(0/1), width, height, framesLow32]
JNIEXPORT jintArray JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_receiverStatus(JNIEnv* env, jobject, jlong h) {
    jintArray out = env->NewIntArray(5);
    Receiver* r = as_receiver(h);
    if (!r) return out;
    const jint vals[5] = {r->connections.load(), r->ptz_supported.load() ? 1 : 0, r->width.load(), r->height.load(),
                          (jint)(r->frames.load() & 0x7fffffff)};
    env->SetIntArrayRegion(out, 0, 5, vals);
    return out;
}

// ---------------------------------------------------------------------------------------------
// PTZ (all values use the NDI conventions, see Processing.NDI.Recv.ex.h)
// ---------------------------------------------------------------------------------------------

#define PTZ_RECV(h) Receiver* r = as_receiver(h); if (!r) return JNI_FALSE

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzPanTiltSpeed(JNIEnv*, jobject, jlong h, jfloat pan, jfloat tilt) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_pan_tilt_speed(r->recv, pan, tilt);
}

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzZoomSpeed(JNIEnv*, jobject, jlong h, jfloat speed) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_zoom_speed(r->recv, speed);
}

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzFocusSpeed(JNIEnv*, jobject, jlong h, jfloat speed) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_focus_speed(r->recv, speed);
}

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzAutoFocus(JNIEnv*, jobject, jlong h) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_auto_focus(r->recv);
}

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzStorePreset(JNIEnv*, jobject, jlong h, jint index) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_store_preset(r->recv, index);
}

JNIEXPORT jboolean JNICALL
Java_it_mobile_bisax_ptzvision_ndi_NdiNative_ptzRecallPreset(JNIEnv*, jobject, jlong h, jint index, jfloat speed) {
    PTZ_RECV(h);
    return g_ndi->recv_ptz_recall_preset(r->recv, index, speed);
}

}  // extern "C"
