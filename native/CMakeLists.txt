cmake_minimum_required(VERSION 3.12)

project(lame4j LANGUAGES C)

set(JDK_8_VERSION "943a5ea328fd2fc8eed0aed4ec9b1957d41f8144") # jdk8u462-ga
set(JNI_HEADER_HASH "ed99792df48670072b78028faf704a8dcb6868fe140ccc7eced9b01dfa62fef4")
set(LAME_VERSION "1f5cc9487284d5950343aa5d4f70de433468070a")
set(MINIMP3_VERSION "afb604c06bc8beb145fecd42c0ceb5bda8795144")

if (WIN32)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/windows/javavm/export/jni_md.h")
elseif (APPLE)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/macosx/javavm/export/jni_md.h")
elseif (UNIX)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/solaris/javavm/export/jni_md.h")
else ()
    message(WARNING "Unknown OS, not compiling jni_md.h")
endif ()

file(DOWNLOAD
        "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/share/javavm/export/jni.h"
        ${CMAKE_BINARY_DIR}/jni_headers/jni.h
        EXPECTED_HASH SHA256=${JNI_HEADER_HASH}
)

if (DEFINED JNI_MD_URL)
    file(DOWNLOAD
            ${JNI_MD_URL}
            ${CMAKE_BINARY_DIR}/jni_headers/jni_md.h
    )
endif ()

include(FetchContent)
FetchContent_Declare(
        lame
        GIT_REPOSITORY https://github.com/lameproject/lame.git
        GIT_TAG ${LAME_VERSION}
        PATCH_COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_LIST_DIR}/cmake/lame/config_patch.h.in ${CMAKE_CURRENT_BINARY_DIR}/_deps/lame-src/config_patch.h.in && ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_LIST_DIR}/cmake/lame/CMakeLists.txt ${CMAKE_CURRENT_BINARY_DIR}/_deps/lame-src/CMakeLists.txt
)
FetchContent_MakeAvailable(lame)

FetchContent_Declare(
        minimp3
        GIT_REPOSITORY https://github.com/lieff/minimp3.git
        GIT_TAG master
)
FetchContent_MakeAvailable(minimp3)

set(MINIMP3_IMPL_C "${CMAKE_BINARY_DIR}/minimp3_impl.c")
file(WRITE "${MINIMP3_IMPL_C}"
        "#define MINIMP3_IMPLEMENTATION\n"
        "#include \"${minimp3_SOURCE_DIR}/minimp3.h\"\n"
)

add_library(minimp3 STATIC "${MINIMP3_IMPL_C}")
set_target_properties(minimp3 PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(lame4j SHARED
        src/encoder.c
        src/decoder.c
        src/exceptions.c
)

target_include_directories(lame4j PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_BINARY_DIR}/jni_headers
        ${lame_SOURCE_DIR}
        ${lame_SOURCE_DIR}/include
        ${lame_SOURCE_DIR}/libmp3lame
        ${lame_SOURCE_DIR}/mpglib

        ${minimp3_SOURCE_DIR}
)

target_link_libraries(lame4j PRIVATE
        mp3lame-static
        minimp3
)

message(STATUS "C compiler executable: ${CMAKE_C_COMPILER}")
