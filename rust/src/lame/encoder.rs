use std::{ptr};
use jni::{JNIEnv};
use jni::objects::{JByteArray, JClass, JObject, JShortArray, JValue};
use jni::sys::{jbyte, jint, jlong, jshort};
use mp3lame_sys::{lame_close, lame_encode_buffer, lame_encode_buffer_interleaved, lame_encode_flush, lame_global_flags, lame_init, lame_init_params, lame_set_brate, lame_set_in_samplerate, lame_set_mode, lame_set_num_channels, lame_set_quality, MPEG_mode};
use libc::{c_uchar};
use crate::lame::exceptions::{throw_illegal_state_exception, throw_io_exception, throw_runtime_exception};

#[allow(dead_code)]
struct LameEncoder {
    lame: *mut lame_global_flags,
    channels: i32,
    sample_rate: i32,
    bit_rate: i32,
    quality: i32,
}

#[no_mangle]
pub unsafe extern "C" fn Java_de_maxhenkel_lame4j_Mp3Encoder_createEncoder(mut env: JNIEnv, _class: JClass, channels: jint, sample_rate: jint, bit_rate: jint, quality: jint) -> jlong {
    let lame = lame_init();

    lame_set_num_channels(lame, channels as i32);
    lame_set_in_samplerate(lame, sample_rate as i32);
    lame_set_brate(lame, bit_rate as i32);
    lame_set_mode(lame, if channels == 1 { MPEG_mode::JOINT_STEREO } else { MPEG_mode::MONO });
    lame_set_quality(lame, quality as i32);
    let i = lame_init_params(lame);

    if i < 0 {
        lame_close(lame);
        throw_io_exception(&mut env, format!("Failed to initialize LAME: {}", i));
        return 0;
    }

    return create_pointer(LameEncoder {
        lame,
        channels,
        sample_rate,
        bit_rate,
        quality,
    });
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Encoder_writeInternal<'a>(mut env: JNIEnv<'a>, obj: JObject<'a>, input: JShortArray<'a>) -> JByteArray<'a> {
    let lame = match get_lame(&mut env, &obj) {
        Some(lame) => lame,
        None => {
            return JByteArray::from(JObject::null());
        }
    };

    let input_length = match env.get_array_length(&input) {
        Ok(input_length) => input_length as usize,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to get input length: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    if input_length % lame.channels as usize != 0 {
        throw_io_exception(&mut env, "Input length must be a multiple of the number of channels");
        return JByteArray::from(JObject::null());
    }

    let mut input_vec = vec![0i16 as jshort; input_length];

    match env.get_short_array_region(input, 0, &mut input_vec) {
        Ok(_) => {}
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to convert short array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    let buffer: Vec<jbyte> = vec![0u8 as jbyte; estimate_mp3_buffer_size(input_length as i32) * lame.channels as usize];

    let num_bytes = if lame.channels == 1 {
        unsafe { lame_encode_buffer(lame.lame, input_vec.as_slice().as_ptr(), ptr::null(), input_length as i32, buffer.as_ptr() as *mut c_uchar, buffer.len() as i32) }
    } else {
        unsafe { lame_encode_buffer_interleaved(lame.lame, input_vec.as_mut_slice().as_mut_ptr(), input_length as i32 / lame.channels, buffer.as_ptr() as *mut c_uchar, buffer.len() as i32) }
    };

    if num_bytes < 0 {
        throw_io_exception(&mut env, format!("Failed to encode samples: {}", num_bytes));
        return JByteArray::from(JObject::null());
    }

    let output = match env.new_byte_array(num_bytes as i32) {
        Ok(arr) => arr,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to create byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    match env.set_byte_array_region(&output, 0, buffer.as_slice()[..num_bytes as usize].as_ref()) {
        Ok(_) => {}
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed populate byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    return output;
}

fn estimate_mp3_buffer_size(num_samples: i32) -> usize {
    return (1.25f64 * num_samples as f64 + 7200f64).ceil() as usize;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Encoder_flush<'a>(mut env: JNIEnv<'a>, obj: JObject<'a>) -> JByteArray<'a> {
    let lame = match get_lame(&mut env, &obj) {
        Some(lame) => lame,
        None => {
            return JByteArray::from(JObject::null());
        }
    };

    let buffer: Vec<jbyte> = vec![0u8 as jbyte; 7200];

    let mum_bytes_flush = unsafe { lame_encode_flush(lame.lame, buffer.as_ptr() as *mut c_uchar, buffer.len() as i32) };

    let output = match env.new_byte_array(mum_bytes_flush as i32) {
        Ok(arr) => arr,
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed to create byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    match env.set_byte_array_region(&output, 0, buffer.as_slice()[..mum_bytes_flush as usize].as_ref()) {
        Ok(_) => {}
        Err(e) => {
            throw_io_exception(&mut env, format!("Failed populate byte array: {}", e));
            return JByteArray::from(JObject::null());
        }
    };

    return output;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Encoder_destroyEncoder(mut env: JNIEnv, obj: JObject) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let lame = unsafe { Box::from_raw(pointer as *mut LameEncoder) };

    unsafe { lame_close(lame.lame) };

    let _ = env.set_field(obj, "lame", "J", JValue::from(jlong::from(0)));
}

fn get_pointer(env: &mut JNIEnv, obj: &JObject) -> jlong {
    let pointer = match env.get_field(obj, "lame", "J") {
        Ok(pointer) => pointer,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to get lame pointer: {}", e));
            return 0;
        }
    };
    let long = match pointer.j() {
        Ok(long) => long,
        Err(e) => {
            throw_runtime_exception(env, format!("Failed to convert lame pointer to long: {}", e));
            return 0;
        }
    };
    return long;
}

fn get_lame_from_pointer(pointer: jlong) -> &'static mut LameEncoder {
    let lame = unsafe { &mut *(pointer as *mut LameEncoder) };
    return lame;
}

fn get_lame(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut LameEncoder> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        throw_illegal_state_exception(env, "Encoder is closed");
        return None;
    }
    return Some(get_lame_from_pointer(pointer));
}

fn create_pointer(lame: LameEncoder) -> jlong {
    let lame = Box::new(lame);
    let raw = Box::into_raw(lame);
    return raw as jlong;
}