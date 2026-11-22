cmake_minimum_required(VERSION 3.22.1)
project(ptzvision_ndi CXX)

# JNI bridge to the NDI SDK. libndi.so is loaded at runtime with dlopen(), so only the
# (MIT licensed) headers in ndi/include are needed to build.
add_library(ndibridge SHARED ndi_bridge.cpp)
target_include_directories(ndibridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/ndi/include)
target_compile_features(ndibridge PRIVATE cxx_std_17)
target_compile_options(ndibridge PRIVATE -Wall -Wno-deprecated-declarations)
target_link_libraries(ndibridge android log dl)
target_link_options(ndibridge PRIVATE "-Wl,-z,max-page-size=16384")
