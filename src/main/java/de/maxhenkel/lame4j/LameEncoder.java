package de.maxhenkel.lame4j;

import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.OutputStream;

public class LameEncoder implements AutoCloseable {

    private final Pointer gfp;
    private final int channels;
    private final OutputStream outputStream;

    public LameEncoder(int channels, int sampleRate, int bitRate, int quality, OutputStream outputStream) throws IOException {
        assert channels == 1 || channels == 2;
        assert quality >= 0 && quality <= 9;
        this.channels = channels;
        this.outputStream = outputStream;
        gfp = Lame.INSTANCE.lame_init();
        Lame.INSTANCE.lame_set_num_channels(gfp, channels);
        Lame.INSTANCE.lame_set_in_samplerate(gfp, sampleRate);
        Lame.INSTANCE.lame_set_brate(gfp, bitRate);
        Lame.INSTANCE.lame_set_mode(gfp, channels == 1 ? Lame.LameMode.MONO : Lame.LameMode.JOINT_STEREO);
        Lame.INSTANCE.lame_set_quality(gfp, quality);
        int result = Lame.INSTANCE.lame_init_params(gfp);
        if (result < 0) {
            throw new IOException("Failed to initialize LAME");
        }
    }

    public void write(short[] samples) throws IOException {
        assert samples.length % channels == 0;
        byte[] buffer = new byte[Lame.INSTANCE.estimateMp3BufferSize(samples.length) * channels];
        int numBytes;
        if (channels == 1) {
            numBytes = Lame.INSTANCE.lame_encode_buffer(gfp, samples, null, samples.length, buffer, buffer.length);
        } else {
            numBytes = Lame.INSTANCE.lame_encode_buffer_interleaved(gfp, samples, samples.length / channels, buffer, buffer.length);
        }
        if (numBytes < 0) {
            throw new IOException("Failed to encode samples");
        }
        outputStream.write(buffer, 0, numBytes);
    }

    @Override
    public void close() throws IOException {
        byte[] flushBuffer = new byte[7200];

        int numBytesFlush = Lame.INSTANCE.lame_encode_flush(gfp, flushBuffer, flushBuffer.length);

        outputStream.write(flushBuffer, 0, numBytesFlush);

        Lame.INSTANCE.lame_close(gfp);
        outputStream.close();
    }
}