package de.maxhenkel.lame4j;

import com.sun.jna.Pointer;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

public class LameDecoder {

    private static final byte[][] MAGIC_BYTES = new byte[][]{
            {(byte) 0xFF, (byte) 0xFB},
            {(byte) 0xFF, (byte) 0xF3},
            {(byte) 0xFF, (byte) 0xF2},
            {(byte) 0x49, (byte) 0x44, (byte) 0x33}
    };

    private final InputStream inputStream;
    private final Lame.Mp3Data mp3Data;

    public LameDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
        this.mp3Data = new Lame.Mp3Data();
    }

    public short[] decode() throws IOException {
        ShortArrayBuffer sampleBuffer = new ShortArrayBuffer(2048);
        decode(sampleBuffer::write);
        return sampleBuffer.toShortArray();
    }

    public void decode(ShortConsumer shortConsumer) throws IOException {
        byte[] buffer = new byte[1024];

        int read = inputStream.read(buffer);
        if (read <= 0) {
            return;
        }

        if (!hasMagicBytes(buffer)) {
            throw new IOException("Invalid file format");
        }

        Pointer gfp = Lame.INSTANCE.lame_init();
        Lame.INSTANCE.lame_set_decode_only(gfp, 1);
        Pointer hip = Lame.INSTANCE.hip_decode_init();

        short[] bufferLeft = new short[8192];
        short[] bufferRight = new short[8192];
        short[] bufferInterleaved = null;

        while (true) {
            read = inputStream.read(buffer);
            if (read <= 0) {
                break;
            }

            while (true) {
                int samplesRead = Lame.INSTANCE.hip_decode1_headers(hip, buffer, read, bufferLeft, bufferRight, mp3Data);
                read = 0;

                if (samplesRead <= 0) {
                    break;
                }

                if (mp3Data.header_parsed == 0) {
                    throw new IOException("Failed to parse MP3 header");
                }

                if (mp3Data.stereo == 1) {
                    shortConsumer.accept(bufferLeft, 0, samplesRead);
                } else {
                    if (bufferInterleaved == null) {
                        bufferInterleaved = new short[bufferLeft.length + bufferRight.length];
                    }
                    for (int i = 0; i < samplesRead; i++) {
                        bufferInterleaved[i * 2] = bufferLeft[i];
                        bufferInterleaved[i * 2 + 1] = bufferRight[i];
                    }
                    shortConsumer.accept(bufferInterleaved, 0, samplesRead * 2);
                }
            }
        }

        Lame.INSTANCE.hip_decode_exit(hip);
        Lame.INSTANCE.lame_close(gfp);
        inputStream.close();
    }

    private boolean hasMagicBytes(byte[] data) {
        for (byte[] magicBytes : MAGIC_BYTES) {
            if (data.length < magicBytes.length) {
                return false;
            }
            boolean valid = true;
            for (int i = 0; i < magicBytes.length; i++) {
                if (data[i] != magicBytes[i]) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return true;
            }
        }
        return false;
    }

    public interface ShortConsumer {
        void accept(short[] samples, int offset, int length);
    }

    public int getSampleRate() {
        return mp3Data.samplerate;
    }

    public int getChannelCount() {
        return mp3Data.stereo;
    }

    public int getBitrate() {
        return mp3Data.bitrate;
    }

    public int getFrameSize() {
        return mp3Data.framesize;
    }

    public int getSampleSizeInBits() {
        return getSampleSizeInBytes() * 8;
    }

    public int getSampleSizeInBytes() {
        return 2;
    }

    public AudioFormat format() {
        return new AudioFormat(getSampleRate(), getSampleSizeInBits(), getChannelCount(), true, false);
    }

}
