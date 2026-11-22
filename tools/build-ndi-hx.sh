#!/usr/bin/env bash
# Build the FFmpeg libraries loaded by the Android NDI 6.4 runtime.
set -euo pipefail

: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the Android NDK directory}"
repo=$(cd "$(dirname "$0")/.." && pwd)
work="$repo/build/ndi-hx"
version=7.1.5
archive="$work/ffmpeg-$version.tar.xz"
source_dir="$work/ffmpeg-$version"
mkdir -p "$work"
if [[ ! -f "$archive" ]]; then
    curl -fL --retry 2 "https://ffmpeg.org/releases/ffmpeg-$version.tar.xz" -o "$archive.part"
    mv "$archive.part" "$archive"
fi
echo "de668509caf9e35e3cd162473441fdb29538c6d96ed080292b3cf9e6fc5d558f  $archive" | shasum -a 256 -c -
if [[ ! -f "$source_dir/configure" ]]; then
    tar -xf "$archive" -C "$work"
fi

toolchains=("$ANDROID_NDK_HOME"/toolchains/llvm/prebuilt/*/bin)
bin=${toolchains[0]}
if [[ $# == 0 ]]; then set -- arm64-v8a armeabi-v7a x86_64; fi
for abi in "$@"; do
    extra=()
    case "$abi" in
        arm64-v8a) arch=aarch64; target=aarch64-linux-android ;;
        armeabi-v7a) arch=arm; target=armv7a-linux-androideabi; extra+=(--cpu=armv7-a) ;;
        x86_64) arch=x86_64; target=x86_64-linux-android; extra+=(--disable-x86asm) ;;
        *) echo "Unsupported Android ABI: $abi" >&2; exit 1 ;;
    esac
    mkdir -p "$work/obj/$abi" "$work/jniLibs/$abi"
    (
        cd "$work/obj/$abi"
        "$source_dir/configure" --target-os=android --arch="$arch" --enable-cross-compile \
            --cc="$bin/${target}29-clang" --ar="$bin/llvm-ar" --nm="$bin/llvm-nm" \
            --ranlib="$bin/llvm-ranlib" --strip="$bin/llvm-strip" \
            --enable-shared --disable-static --enable-pic --disable-everything \
            --disable-autodetect --disable-doc --disable-programs --disable-debug \
            --disable-avdevice --disable-avformat --disable-avfilter --disable-postproc \
            --disable-swscale --disable-swresample --disable-network \
            --enable-decoder=h264,hevc,aac --enable-parser=h264,hevc,aac \
            --extra-ldflags=-Wl,-z,max-page-size=16384 ${extra[@]+"${extra[@]}"}
        # Android packages unversioned filenames. NDI uses the major-version SONAMEs.
        sed '/^SHFLAGS=/s/-soname,$(SLIBNAME)/-soname,$(SLIBNAME_WITH_MAJOR)/' \
            ffbuild/config.mak > ffbuild/config.mak.tmp
        mv ffbuild/config.mak.tmp ffbuild/config.mak
        make -j"${JOBS:-6}"
        for lib in avutil avcodec; do
            "$bin/llvm-strip" --strip-unneeded -o "$work/jniLibs/$abi/lib$lib.so" "lib$lib/lib$lib.so"
        done
    )
done
mkdir -p "$work/assets/licenses"
cp "$source_dir/COPYING.LGPLv2.1" "$work/assets/licenses/FFmpeg-LGPL-2.1.txt"
