#include <jni.h>
#include <lame.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>
#include <stdbool.h>

#include "exceptions.h"

#define FLUSH_BUFFER_SIZE 7200

typedef struct Encoder {
    lame_global_flags *lame;
    jint channels;
    jint sample_rate;
    jint bit_rate;
    jint quality;
} Encoder;

/**
 * Gets the encoder from the encoder java object.
 *
 * @param env the JNI environment
 * @param encoder_pointer the pointer to the encoder
 * @return the encoder or NULL - If the encoder could not be retrieved, this will throw a runtime exception in Java
 */
Encoder *get_encoder(JNIEnv *env, const jlong encoder_pointer) {
    const jlong pointer = encoder_pointer;
    if (pointer == 0) {
        throw_runtime_exception(env, "Encoder is closed");
        return NULL;
    }
    return (Encoder *) (uintptr_t) pointer;
}

JNIEXPORT jlong JNICALL Java_de_maxhenkel_lame4j_Mp3Encoder_createEncoder0(
    JNIEnv *env,
    jclass clazz,
    const jint channels,
    const jint sample_rate,
    const jint bit_rate,
    const jint quality
) {
    if (channels != 1 && channels != 2) {
        char *message = string_format("Invalid number of channels: %d", channels);
        throw_illegal_argument_exception(env, message);
        free(message);
        return 0;
    }

    lame_global_flags *lame = lame_init();

    if (lame == NULL) {
        throw_io_exception(env, "Failed to initialize LAME encoder");
        return 0;
    }

    lame_set_num_channels(lame, channels);
    lame_set_in_samplerate(lame, sample_rate);
    lame_set_brate(lame, bit_rate);
    lame_set_mode(lame, channels == 1 ? MONO : JOINT_STEREO);
    lame_set_quality(lame, quality);

    int i = lame_init_params(lame);

    if (i < 0) {
        lame_close(lame);
        throw_io_exception(env, "Failed to initialize LAME parameters");
        return 0;
    }

    Encoder *encoder = malloc(sizeof(Encoder));
    encoder->lame = lame;
    encoder->channels = channels;
    encoder->sample_rate = sample_rate;
    encoder->bit_rate = bit_rate;
    encoder->quality = quality;

    return (jlong) (uintptr_t) encoder;
}

int32_t estimate_mp3_buffer_size(const int32_t num_samples) {
    return (int32_t) ceil(1.25 * (double) num_samples + 7200.0);
}

JNIEXPORT jbyteArray JNICALL Java_de_maxhenkel_lame4j_Mp3Encoder_writeInternal0(
    JNIEnv *env,
    jobject obj,
    const jlong encoder_pointer,
    const jshortArray input
) {
    const Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return NULL;
    }
    const jint input_length = (*env)->GetArrayLength(env, input);

    if (input_length % (encoder->channels) != 0) {
        throw_illegal_argument_exception(env, "Input length must be a multiple of the number of channels");
        return NULL;
    }

    const int32_t buffer_size = estimate_mp3_buffer_size(input_length) * encoder->channels;
    unsigned char *buffer = calloc(buffer_size, sizeof(char));

    jshort *lame_input = (*env)->GetShortArrayElements(env, input, false);

    int result;
    if (encoder->channels == 1) {
        result = lame_encode_buffer(encoder->lame, lame_input, NULL, input_length, buffer, buffer_size);
    } else {
        result = lame_encode_buffer_interleaved(encoder->lame, lame_input, input_length / encoder->channels, buffer,
                                                buffer_size);
    }

    (*env)->ReleaseShortArrayElements(env, input, lame_input, JNI_ABORT);

    if (result < 0) {
        throw_io_exception(env, "Failed to encode samples");
        free(buffer);
        return NULL;
    }

    const jbyteArray java_output = (*env)->NewByteArray(env, result);
    (*env)->SetByteArrayRegion(env, java_output, 0, result, (jbyte *) buffer);
    free(buffer);
    return java_output;
}

JNIEXPORT jbyteArray JNICALL Java_de_maxhenkel_lame4j_Mp3Encoder_flush0(
    JNIEnv *env,
    jobject obj,
    const jlong encoder_pointer
) {
    const Encoder *encoder = get_encoder(env, encoder_pointer);
    if (encoder == NULL) {
        return NULL;
    }

    unsigned char *buffer = calloc(FLUSH_BUFFER_SIZE, sizeof(char));

    const int result = lame_encode_flush(encoder->lame, buffer, FLUSH_BUFFER_SIZE);

    if (result < 0) {
        throw_io_exception(env, "Failed to flush encoder");
        free(buffer);
        return NULL;
    }

    const jbyteArray java_output = (*env)->NewByteArray(env, result);
    (*env)->SetByteArrayRegion(env, java_output, 0, result, (jbyte *) buffer);
    free(buffer);
    return java_output;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_lame4j_Mp3Encoder_destroyEncoder0(
    JNIEnv *env,
    jobject obj,
    const jlong encoder_pointer
) {
    if (encoder_pointer == 0) {
        return;
    }
    Encoder *encoder = (Encoder *) (uintptr_t) encoder_pointer;
    lame_close(encoder->lame);
    free(encoder);
}
