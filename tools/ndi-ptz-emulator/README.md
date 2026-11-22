# NDI® PTZ camera emulator

A small NDI sender that behaves like a PTZ camera. It is used to test PTZ Vision's NDI support without real hardware.

* It advertises PTZ support (`<ndi_capabilities ntk_ptz="true"/>`).
* It renders a synthetic scene that follows the virtual pan, tilt, zoom and focus. Manual focus out of the "right"
  distance blurs the grid lines. Two bars show the zoom (yellow) and focus (cyan) levels.
* It stores and recalls presets 0–99. A green border flashes on each preset action.
* It logs every PTZ message it receives, for example
  `[cmd #12] <ntk_ptz_pan_tilt_speed pan_speed="-0.40" tilt_speed="0.000000"/>`.

## Build and run (Linux, e.g. Raspberry Pi 4/5 64-bit)

You need the *NDI SDK for Linux*. Installing it means accepting the NDI SDK license.

```sh
curl -LO https://downloads.ndi.tv/SDK/NDI_SDK_Linux/Install_NDI_SDK_v6_Linux.tar.gz
tar xzf Install_NDI_SDK_v6_Linux.tar.gz && sh Install_NDI_SDK_v6_Linux.sh   # shows the EULA

make NDI_SDK_DIR="$PWD/NDI SDK for Linux"                    # default NDI_ARCH = aarch64-rpi4-linux-gnueabi
./ndi_ptz_emulator "PTZ-EMU-1" 960 540                       # [name] [width] [height]
```

On x86_64 hosts, build with `make NDI_ARCH=x86_64-linux-gnu`.

The source shows up on the network as `HOSTNAME (PTZ-EMU-1)`. Run several instances with different names to emulate
several cameras. On a Raspberry Pi 4, each 960×540 instance uses about 45 % of one core.
