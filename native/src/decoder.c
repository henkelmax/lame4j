#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <minimp3.h>

#include "exceptions.h"

typedef struct Decoder {
    mp3dec_t *mp3dec;
    jint channels;
    jint sample_rate;
    jint bit_rate;
} Decoder;

/**
 * Gets the decoder from the decoder java object.
 *
 * @param env the JNI environment
 * @param decoder_pointer the pointer to the decoder
 * @return the decoder or NULL - If the decoder could not be retrieved, this will throw a runtime exception in Java
 */
Decoder *get_decoder(JNIEnv *env, const jlong decoder_pointer) {
    if (decoder_pointer == 0) {
        throw_runtime_exception(env, "Decoder is closed");
        return NULL;
    }
    return (Decoder *) (uintptr_t) decoder_pointer;
}


JNIEXPORT jlong JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_createDecoder0(
    JNIEnv *env,
    jclass clazz
) {
    mp3dec_t *mp3dec = malloc(sizeof(mp3dec_t));
    mp3dec_init(mp3dec);
    Decoder *decoder = malloc(sizeof(Decoder));
    decoder->mp3dec = mp3dec;
    decoder->channels = -1;
    decoder->sample_rate = -1;
    decoder->bit_rate = -1;

    return (jlong) (uintptr_t) decoder;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_getMaxSamplesPerFrame0(
    JNIEnv *env,
    jclass clazz
) {
    return MINIMP3_MAX_SAMPLES_PER_FRAME;
}

JNIEXPORT jlong JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_decodeNextFrame0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer,
    const jbyteArray input,
    const jint input_length,
    const jshortArray output
) {
    Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return 0;
    }

    const jsize java_output_length = (*env)->GetArrayLength(env, output);

    if (java_output_length < MINIMP3_MAX_SAMPLES_PER_FRAME) {
        throw_illegal_argument_exception(env, "Output array is too small");
        return 0;
    }

    const jsize actual_input_length = (*env)->GetArrayLength(env, input);

    if (input_length < 0 || input_length > actual_input_length) {
        throw_illegal_argument_exception(env, "Input length is too small");
        return 0;
    }

    const uint8_t *mp3_input = (uint8_t *) (*env)->GetByteArrayElements(env, input, false);

    mp3d_sample_t *audio_output = calloc(MINIMP3_MAX_SAMPLES_PER_FRAME, sizeof(mp3d_sample_t));

    mp3dec_frame_info_t frame_info;
    const int frames_used = mp3dec_decode_frame(decoder->mp3dec, mp3_input, input_length, audio_output, &frame_info);
    (*env)->ReleaseByteArrayElements(env, input, (jbyte *) mp3_input, JNI_ABORT);

    // If the frame bytes are > 0, we can be sure the header has been parsed
    if (frame_info.frame_bytes > 0) {
        decoder->channels = frame_info.channels;
        decoder->sample_rate = frame_info.hz;
        decoder->bit_rate = frame_info.bitrate_kbps;
    }

    if (frames_used <= 0) {
        free(audio_output);
        return (jlong) frame_info.frame_bytes & 0xFFFFFFFFL;
    }

    (*env)->SetShortArrayRegion(env, output, 0, frames_used * decoder->channels, audio_output);
    free(audio_output);

    const int32_t frames = frames_used * decoder->channels;
    const int32_t bytes = frame_info.frame_bytes;

    return (jlong) (jint) frames << 32 | (jlong) bytes & 0xFFFFFFFFL;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_getChannelCount0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return -1;
    }
    return decoder->channels;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_getBitRate0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return -1;
    }
    return decoder->bit_rate;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_getSampleRate0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer
) {
    const Decoder *decoder = get_decoder(env, decoder_pointer);
    if (decoder == NULL) {
        return -1;
    }
    return decoder->sample_rate;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_lame4j_Mp3Decoder_destroyDecoder0(
    JNIEnv *env,
    jobject obj,
    const jlong decoder_pointer
) {
    if (decoder_pointer == 0) {
        return;
    }
    Decoder *decoder = (Decoder *) (uintptr_t) decoder_pointer;
    free(decoder->mp3dec);
    free(decoder);
}
