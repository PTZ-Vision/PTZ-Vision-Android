# NDI® support in PTZ Vision

> NDI® is a registered trademark of Vizrt NDI AB. PTZ Vision is not affiliated with or endorsed by Vizrt; it is
> *compatible with* NDI through the official NDI SDK. More info: <https://ndi.video>.

## 1. What NDI is

**NDI (Network Device Interface)** is a protocol suite for live video over ordinary IP/Ethernet networks, created by
NewTek (2015) and now owned by Vizrt (NDI AB). It is proprietary but free to use through the NDI SDK.

### How it works

| Layer | What NDI does |
|---|---|
| **Discovery** | Each source is advertised via **mDNS/DNS-SD** as `_ndi._tcp` with the name `MACHINE (SOURCE)`. On networks without multicast you can use an **NDI Discovery Server** or point the finder to explicit IPs (`p_extra_ips`). |
| **Transport** | A sender/receiver connection per source. Since NDI 5 the default is **Reliable UDP (RUDP)** with congestion control; multi‑TCP, single TCP and multicast are also available. |
| **Video codecs** | **NDI (High Bandwidth)**: SpeedHQ, an intra-frame DCT codec, ~125 Mbps for 1080p60 and about 1 frame of latency. **NDI\|HX2/HX3**: H.264/H.265 long-GOP, 8–30 Mbps, slightly higher latency. Every sender also offers a **low-bandwidth proxy** stream (~640×360) on request. |
| **Audio** | Uncompressed float multichannel, frame-accurate with video. |
| **Metadata** | A bidirectional XML channel on the same connection. It carries capabilities, **PTZ commands**, **tally** and web-control URLs. |

### PTZ over NDI

PTZ commands are XML metadata messages that the receiver sends back to the source. The SDK wraps them in
`NDIlib_recv_ptz_*()` calls:

| SDK call | Message | Range / notes |
|---|---|---|
| `recv_ptz_pan_tilt_speed` | `<ntk_ptz_pan_tilt_speed pan_speed tilt_speed/>` | -1..1. **pan_speed > 0 = left**, tilt_speed > 0 = up |
| `recv_ptz_pan_tilt` | `<ntk_ptz_pan_tilt pan tilt/>` | absolute -1..1 |
| `recv_ptz_zoom_speed` | `<ntk_ptz_zoom_speed zoom_speed/>` | -1 (out) .. 1 (in) |
| `recv_ptz_zoom` | `<ntk_ptz_zoom zoom/>` | 0 (tele) .. 1 (wide) |
| `recv_ptz_focus_speed` | `<ntk_ptz_focus_speed …/>` | -1 (far) .. 1 (near) |
| `recv_ptz_auto_focus` / `recv_ptz_focus` | `<ntk_ptz_focus mode="auto"|"manual" distance/>` | |
| `recv_ptz_store_preset` / `recv_ptz_recall_preset` | `<ntk_ptz_store_preset index/>`, `<ntk_ptz_recall_preset index speed/>` | index 0..99 |
| white balance / exposure | `<ntk_ptz_white_balance …/>`, `<ntk_ptz_exposure …/>` | |

A source advertises PTZ support with the connection metadata `<ndi_capabilities ntk_ptz="true"/>`, which a
receiver reads through `NDIlib_recv_ptz_is_supported()`.

**Limit:** PTZ over NDI is **one-way**. There is no standard message to read the current pan, tilt or zoom position,
the focus mode or presets. That is why the zoom indicator shows `--x` for NDI cameras.

### NDI compared with VISCA-over-IP + RTSP (what PTZ Vision used before)

| | VISCA-over-IP + RTSP | NDI |
|---|---|---|
| Setup | IP address and ports typed by hand, per camera and per protocol | **Automatic discovery** by name, and the name stays valid when the IP changes (DHCP) |
| Connections | 2 or 3 sockets (VISCA TCP/UDP, RTSP/RTP, HTTP) | **1 connection** for video, audio, PTZ and tally |
| Video latency | RTSP H.264 long-GOP: usually 150–500 ms (jitter buffers, GOP) | NDI full-bandwidth about 1 frame; HX about 2–4 frames |
| Mobile-friendly preview | Main or sub stream must be configured on the camera | **Proxy stream on demand** (`bandwidth_lowest`) |
| Vendor interoperability | VISCA dialects differ between vendors | Standardised message set, implemented by every NDI PTZ camera |
| Feedback / state queries | Inquiry commands (zoom position, etc.) | Not available (one-way) |
| Openness | Open/documented | Proprietary SDK, closed-source runtime with an EULA |
| Network load | Low (H.264) | Full NDI is high (~100+ Mbps 1080p). Proxy/HX is low |

## 2. Feasibility analysis (summary)

| Feature | Feasible? | How |
|---|---|---|
| Discovery | ✅ fully | NDI SDK finder; **without the SDK** plain mDNS (`NsdManager`, `_ndi._tcp`) |
| PTZ control | ✅ only through the SDK | The connection handshake is proprietary and reverse-engineering it is forbidden by the EULA, so the official runtime is required |
| Video | ✅ | SDK decode to RGBX, copied into the `SurfaceView`'s `ANativeWindow` |
| Build without proprietary binaries | ✅ | `libndi.so` is loaded at runtime with `dlopen` and `NDIlib_v6_load()`. Only the **MIT-licensed headers** are in the repo |

Risks found and how they are handled:

* **NDI\|HX on Android:** decoding HX sources on Android has known issues. If no frame arrives within 12 s the preview
  falls back to RTSP when an IP is configured.
* **NsdManager:** the SDK needs a live `NsdManager` on Android. `NdiRuntime` holds one for the whole process.
* **Licensing:** see section 5.

## 3. Architecture

```
MainActivity ──► NdiRuntime.init()        dlopen(libndi.so) + keep NsdManager alive
                    │
Settings ──► NdiDiscoveryScreen ──► NdiDiscovery ──► SDK finder  (or mDNS fallback)
                    │ "Add" (prefilled form: protocol = NDI, source name, IP)
                    ▼
               Room: cam.protocol = NDI, cam.ndiSourceName = "MACHINE (SOURCE)"

Console ──► MainViewModel.createController(cam)
               ├─ VISCA ─► ViscaPTZController (unchanged)
               └─ NDI   ─► NdiPTZController ─┐
         SelectedCam ─► NdiVideoPreview ─────┼─► NdiReceiverPool (1 shared receiver per source,
                                             │     ref-counted) ─► JNI ndi_bridge.cpp
                                             └─► capture thread → ANativeWindow (RGBA)
```

* `app/src/main/cpp/ndi_bridge.cpp` contains the whole JNI layer (~300 lines): finder, receiver, capture thread and
  PTZ calls.
* `app/src/main/cpp/ndi/include/` holds the NDI SDK headers (MIT license, see the header comments).
* The receiver connects **by NDI name**, not by cached `ip:port`, so the SDK always resolves the current address.
* **Database v11:** new columns `protocol` (`VISCA`/`NDI`) and `ndiSourceName`, and the unique index now includes
  `ndiSourceName`. A real migration (10 → 11) keeps the existing cameras.
* **Settings:** an "NDI" button next to "Add" opens discovery. The add/edit form has a VISCA/NDI selector. The "NDI®"
  section has the *Full quality NDI preview* switch (off = proxy stream, recommended on Wi-Fi).

## 4. Building with NDI

The NDI runtime is **not** in the repository. Without it the app builds and runs normally, but NDI cameras show
"NDI runtime not available", discovery uses mDNS only, and PTZ/video over NDI are disabled.

1. Download the *NDI SDK for Android* from <https://ndi.video/for-developers/ndi-sdk/>. The Linux/macOS installer is
   `Install_NDI_SDK_v6_Android.tar.gz`. Running the installer means **accepting the NDI SDK license agreement**.
2. Point the build to it in `local.properties` (forward slashes) or with the `NDI_SDK_DIR` environment variable:
   ```properties
   ndi.sdk.dir=C:/path/to/NDI SDK for Android
   ```
3. Build as usual. `lib/<abi>/libndi.so` is packaged (`libndifile.so` is excluded) and
   `BuildConfig.NDI_RUNTIME_BUNDLED` is `true`.

The CI workflow (`build-release.yml`) does not download the SDK, so the public release APK does **not** include NDI.

## 5. Licensing notes (read before distributing an APK with `libndi.so`)

From the NDI SDK License Agreement and the SDK documentation:

* The SDK **headers** may be distributed under the MIT license, which is why they are in this repo.
* The runtime (`libndi.so`) may be redistributed **inside your app** only. The rest of the SDK (docs, examples, tools)
  must not be redistributed.
* An app that ships the runtime must, in its own license or EULA:
  * prohibit reverse engineering and modification of the NDI SDK;
  * disclaim warranties on behalf of NDI;
  * include NDI's copyright notice.
* Trademarks: use "NDI®" only to state compatibility, with the notice that it is a trademark of Vizrt NDI AB. Do not
  suggest endorsement. The app shows this notice in the discovery screen and in Settings.
* Keep the bundled SDK reasonably up to date (the license requires using the latest SDK version when distributing).

This summary is not legal advice: check the full `NDI SDK License Agreement` shipped with the SDK before publishing.

## 6. Testing without a PTZ camera

`tools/ndi-ptz-emulator/` contains a small NDI sender that behaves like a PTZ camera. It advertises `ntk_ptz`, renders
a scene that follows pan, tilt, zoom and focus, stores and recalls presets, and logs every command it receives. It was
used to validate PTZ Vision on a Raspberry Pi 4. See its README.
