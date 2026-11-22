# Build the NDI PTZ emulator against an installed NDI SDK for Linux.
#   make NDI_SDK_DIR="$HOME/ndi/NDI SDK for Linux"
# NDI_ARCH defaults to the Raspberry Pi 4/5 64-bit build of the runtime.

NDI_SDK_DIR ?= $(HOME)/ndi/NDI SDK for Linux
NDI_ARCH    ?= aarch64-rpi4-linux-gnueabi

NDI_INC := $(NDI_SDK_DIR)/include
NDI_LIB := $(NDI_SDK_DIR)/lib/$(NDI_ARCH)

ndi_ptz_emulator: ndi_ptz_emulator.cpp
	$(CXX) -O2 -std=c++17 -Wall -I"$(NDI_INC)" $< -o $@ -L"$(NDI_LIB)" -lndi -ldl -lpthread -Wl,-rpath,"$(NDI_LIB)"

clean:
	rm -f ndi_ptz_emulator

.PHONY: clean
