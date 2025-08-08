cmake_minimum_required(VERSION 3.15)
project(lame LANGUAGES C)

option(LAME_MPG "Build mpglib support" OFF)
option(LAME_ASM "Include assembly sources" OFF)
option(LAME_SNDFILE "Enable libsndfile" OFF)

set(CMAKE_C_STANDARD 17)
set(CMAKE_C_EXTENSIONS OFF)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(ADDITIONAL_C_FLAGS "")
if(NOT WIN32)
    list(APPEND ADDITIONAL_C_FLAGS "-Wno-implicit-function-declaration" "-Wno-int-conversion")
endif()

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/config_patch.h.in")
    configure_file("${CMAKE_CURRENT_SOURCE_DIR}/config_patch.h.in"
            "${CMAKE_CURRENT_SOURCE_DIR}/config.h" @ONLY)
else()
    message(FATAL_ERROR "Missing config_patch.h.in in ${CMAKE_CURRENT_SOURCE_DIR}.")
endif()

set(SRC_PATH ${CMAKE_CURRENT_SOURCE_DIR})

set(LAMELIB_SRC
        ${SRC_PATH}/libmp3lame/bitstream.c
        ${SRC_PATH}/libmp3lame/encoder.c
        ${SRC_PATH}/libmp3lame/fft.c
        ${SRC_PATH}/libmp3lame/gain_analysis.c
        ${SRC_PATH}/libmp3lame/id3tag.c
        ${SRC_PATH}/libmp3lame/lame.c
        ${SRC_PATH}/libmp3lame/newmdct.c
        ${SRC_PATH}/libmp3lame/psymodel.c
        ${SRC_PATH}/libmp3lame/quantize.c
        ${SRC_PATH}/libmp3lame/quantize_pvt.c
        ${SRC_PATH}/libmp3lame/vector/xmm_quantize_sub.c
        ${SRC_PATH}/libmp3lame/set_get.c
        ${SRC_PATH}/libmp3lame/vbrquantize.c
        ${SRC_PATH}/libmp3lame/reservoir.c
        ${SRC_PATH}/libmp3lame/tables.c
        ${SRC_PATH}/libmp3lame/takehiro.c
        ${SRC_PATH}/libmp3lame/util.c
        ${SRC_PATH}/libmp3lame/mpglib_interface.c
        ${SRC_PATH}/libmp3lame/VbrTag.c
        ${SRC_PATH}/libmp3lame/presets.c
        ${SRC_PATH}/libmp3lame/version.c
)

if(LAME_MPG)
    set(MPGLIB_SRC
            ${SRC_PATH}/mpglib/common.c
            ${SRC_PATH}/mpglib/dct64_i386.c
            ${SRC_PATH}/mpglib/decode_i386.c
            ${SRC_PATH}/mpglib/layer1.c
            ${SRC_PATH}/mpglib/layer2.c
            ${SRC_PATH}/mpglib/layer3.c
            ${SRC_PATH}/mpglib/tabinit.c
            ${SRC_PATH}/mpglib/interface.c
    )
else()
    set(MPGLIB_SRC "")
endif()

add_library(mp3lame-static STATIC ${LAMELIB_SRC} ${MPGLIB_SRC})

target_include_directories(mp3lame-static PUBLIC
        "$<BUILD_INTERFACE:${SRC_PATH}>"
        "$<BUILD_INTERFACE:${SRC_PATH}/include>"
        "$<BUILD_INTERFACE:${SRC_PATH}/libmp3lame>"
)

set(LAME_MACRO "USE_LAYER_2" "HAVE_CONFIG_H" "HAVE_STDINT_H" "TAKEHIRO_IEEE754_HACK")
if(LAME_MPG)
    list(APPEND LAME_MACRO "HAVE_MPGLIB")
endif()
if(LAME_SNDFILE)
    list(APPEND LAME_MACRO "LIBSNDFILE")
endif()
if(LAME_ASM)
    list(APPEND LAME_MACRO "HAVE_NASM")
endif()

target_compile_definitions(mp3lame-static PUBLIC ${LAME_MACRO})
target_compile_options(mp3lame-static PRIVATE ${ADDITIONAL_C_FLAGS})
set_target_properties(mp3lame-static PROPERTIES POSITION_INDEPENDENT_CODE ON)

message(STATUS "Configured LAME: mp3lame-static (LAME_MPG=${LAME_MPG}, LAME_ASM=${LAME_ASM}, LAME_SNDFILE=${LAME_SNDFILE})")
