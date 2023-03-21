use std::io::{ErrorKind, Read, Result};
use jni::{JNIEnv};
use jni::objects::{JClass, JObject, JShortArray, JValue};
use jni::sys::{jboolean, jint, jlong};
use minimp3::{Decoder, Error};

struct DecoderWrapper {
    decoder: Decoder<JavaInputStream>,
    channels: Option<i32>,
    sample_rate: Option<i32>,
    bit_rate: Option<i32>,
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_createDecoder(env: JNIEnv<'static>, _class: JClass) -> jlong {
    let stream_wrapper = JavaInputStream {
        env,
        input_stream: None,
    };

    let decoder = Decoder::new(stream_wrapper);

    let decoder_wrapper = DecoderWrapper {
        decoder,
        channels: None,
        sample_rate: None,
        bit_rate: None,
    };

    return create_pointer(decoder_wrapper);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_decodeNextFrame<'a>(mut env: JNIEnv<'static>, obj: JObject<'a>, stream: JObject<'static>) -> JShortArray<'a> {
    let decoder_wrapper = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return JShortArray::from(JObject::null());
        }
    };

    decoder_wrapper.decoder.reader_mut().input_stream = Some(stream);

    let frame = match decoder_wrapper.decoder.next_frame() {
        Ok(frame) => frame,
        Err(Error::Eof) => {
            return JShortArray::from(JObject::null());
        }
        Err(e) => {
            let _ = env.throw(("java/io/IOException", format!("Failed to decode frame: {}", e)));
            return JShortArray::from(JObject::null());
        }
    };

    decoder_wrapper.channels = Some(frame.channels as i32);
    decoder_wrapper.sample_rate = Some(frame.sample_rate as i32);
    decoder_wrapper.bit_rate = Some(frame.bitrate as i32);

    let short_array = match env.new_short_array(frame.data.len() as i32) {
        Ok(array) => array,
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed to create short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    };
    match env.set_short_array_region(&short_array, 0, frame.data.as_slice()) {
        Ok(_) => {}
        Err(e) => {
            throw_runtime_exception(&mut env, format!("Failed populate short array: {}", e));
            return JShortArray::from(JObject::null());
        }
    }
    return short_array;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_headersParsed(mut env: JNIEnv, obj: JObject) -> jboolean {
    let decoder_wrapper = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return false as jboolean;
        }
    };
    return decoder_wrapper.channels.is_some() as jboolean;
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_getChannelCount(mut env: JNIEnv, obj: JObject) -> jint {
    let decoder_wrapper = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return 0;
        }
    };
    return decoder_wrapper.channels.unwrap_or(-1);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_getBitRate(mut env: JNIEnv, obj: JObject) -> jint {
    let decoder_wrapper = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return 0;
        }
    };
    return decoder_wrapper.bit_rate.unwrap_or(-1);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_getSampleRate(mut env: JNIEnv, obj: JObject) -> jint {
    let decoder_wrapper = match get_decoder(&mut env, &obj) {
        Some(decoder) => decoder,
        None => {
            return 0;
        }
    };
    return decoder_wrapper.sample_rate.unwrap_or(-1);
}

#[no_mangle]
pub extern "C" fn Java_de_maxhenkel_lame4j_Mp3Decoder_destroyDecoder(mut env: JNIEnv, obj: JObject) {
    let pointer = get_pointer(&mut env, &obj);

    if pointer == 0 {
        return;
    }

    let _ = unsafe { Box::from_raw(pointer as *mut DecoderWrapper) };
    let _ = env.set_field(obj, "decoder", "J", JValue::from(jlong::from(0)));
}

struct JavaInputStream {
    env: JNIEnv<'static>,
    input_stream: Option<JObject<'static>>,
}

impl Read for JavaInputStream {
    fn read(&mut self, buf: &mut [u8]) -> Result<usize> {
        if buf.len() <= 0 {
            return Ok(0);
        }

        let byte_array = match self.env.new_byte_array(buf.len() as i32) {
            Ok(byte_array) => byte_array,
            Err(e) => {
                return Err(std::io::Error::new(ErrorKind::Other, e));
            }
        };

        let array_obj = unsafe { JObject::from_raw(byte_array.as_raw()) };
        let jvalue: JValue = JValue::Object(&array_obj);

        let input_stream = match &self.input_stream {
            Some(input_stream) => input_stream,
            None => {
                return Err(std::io::Error::new(ErrorKind::Other, "Input stream unavailable"));
            }
        };

        let value = match self.env.call_method(input_stream, "read", "([B)I", &[jvalue]) {
            Ok(value) => value,
            Err(e) => {
                return Err(std::io::Error::new(ErrorKind::Other, e));
            }
        };
        let num_bytes_read = match value.i() {
            Ok(num_bytes_read) => num_bytes_read,
            Err(e) => {
                return Err(std::io::Error::new(ErrorKind::Other, e));
            }
        };

        if num_bytes_read <= 0 {
            return Ok(0);
        }

        let vec = match self.env.convert_byte_array(byte_array) {
            Ok(vec) => vec,
            Err(e) => {
                return Err(std::io::Error::new(ErrorKind::Other, e));
            }
        };

        buf[..num_bytes_read as usize].copy_from_slice(&vec[..num_bytes_read as usize]);

        return Ok(num_bytes_read as usize);
    }
}

fn get_pointer(env: &mut JNIEnv, obj: &JObject) -> jlong {
    let pointer = match env.get_field(obj, "decoder", "J") {
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

fn get_decoder_from_pointer(pointer: jlong) -> &'static mut DecoderWrapper {
    let lame = unsafe { &mut *(pointer as *mut DecoderWrapper) };
    return lame;
}

fn get_decoder(env: &mut JNIEnv, obj: &JObject) -> Option<&'static mut DecoderWrapper> {
    let pointer = get_pointer(env, obj);
    if pointer == 0 {
        let _ = env.throw(("java/lang/IllegalStateException", "Decoder is closed"));
        return None;
    }
    return Some(get_decoder_from_pointer(pointer));
}

fn create_pointer(lame: DecoderWrapper) -> jlong {
    let lame = Box::new(lame);
    let raw = Box::into_raw(lame);
    return raw as jlong;
}

fn throw_runtime_exception(env: &mut JNIEnv, message: String) {
    let _ = env.throw(("java/lang/RuntimeException", message));
}