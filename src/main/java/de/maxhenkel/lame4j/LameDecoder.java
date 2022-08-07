package de.maxhenkel.lame4j;

import com.sun.jna.Pointer;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

public class LameDecoder {

    private final Pointer gfp;
    private final Pointer hip;
    private final InputStream inputStream;
    private final Lame.Mp3Data mp3Data;

    public LameDecoder(InputStream inputStream) {
        this.inputStream = inputStream;
        gfp = Lame.INSTANCE.lame_init();
        Lame.INSTANCE.lame_set_decode_only(gfp, 1);
        hip = Lame.INSTANCE.hip_decode_init();
        mp3Data = new Lame.Mp3Data();
    }

    public short[] decode() throws IOException {
        ShortArrayBuffer sampleBuffer = new ShortArrayBuffer(2048);
        decode(sampleBuffer::write);
        return sampleBuffer.toShortArray();
    }

    public void decode(ShortConsumer shortConsumer) throws IOException {
        byte[] buffer = new byte[1024];

        short[] bufferLeft = new short[8192];
        short[] bufferRight = new short[8192];
        short[] bufferInterleaved = null;

        while (true) {
            int read = inputStream.read(buffer);
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
