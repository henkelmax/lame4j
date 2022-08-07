package de.maxhenkel.lame4j;

import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.OutputStream;

public class LameEncoder implements AutoCloseable {

    private final Pointer gfp;
    private final int channels;
    private final OutputStream outputStream;

    /**
     * @param channels     the number of channels of the audio data - Valid values are 1 and 2
     * @param sampleRate   the sample rate of the audio data
     * @param bitRate      the target bit rate of the encoded audio data
     * @param quality      the quality of the encoded audio data - Valid values are 0 (highest) to 9 (lowest)
     * @param outputStream the output stream to write the encoded audio data to
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Writes the given samples to the output stream.
     *
     * @param samples the samples to write
     * @throws IOException if an I/O error occurs
     */
    public void write(short[] samples) throws IOException {
        assert samples.length % channels == 0;
        byte[] buffer = new byte[estimateMp3BufferSize(samples.length) * channels];
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

    /**
     * Finalizes the mp3 file and closes the output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        byte[] flushBuffer = new byte[7200];

        int numBytesFlush = Lame.INSTANCE.lame_encode_flush(gfp, flushBuffer, flushBuffer.length);

        outputStream.write(flushBuffer, 0, numBytesFlush);

        Lame.INSTANCE.lame_close(gfp);
        outputStream.close();
    }

    /**
     * @param numSamples the number of PCM samples in each channel - It is not the sum of the number of samples in the L and R channels
     * @return the worst case size of the mp3Buffer
     */
    private int estimateMp3BufferSize(int numSamples) {
        return (int) Math.ceil(1.25D * (double) numSamples + 7200D);
    }

}
