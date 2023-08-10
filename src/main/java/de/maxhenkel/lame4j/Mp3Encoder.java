package de.maxhenkel.lame4j;

import java.io.IOException;
import java.io.OutputStream;

public class Mp3Encoder implements AutoCloseable {

    private long lame;
    private final OutputStream outputStream;

    /**
     * @param channels     the number of channels of the audio data - Valid values are 1 and 2
     * @param sampleRate   the sample rate of the audio data
     * @param bitRate      the target bit rate of the encoded audio data
     * @param quality      the quality of the encoded audio data - Valid values are 0 (highest) to 9 (lowest)
     * @param outputStream the output stream to write the encoded audio data to
     * @throws IOException              if an I/O error occurs
     * @throws UnknownPlatformException if the operating system is not supported
     */
    public Mp3Encoder(int channels, int sampleRate, int bitRate, int quality, OutputStream outputStream) throws IOException, UnknownPlatformException {
        Lame.load();
        this.lame = createEncoder0(channels, sampleRate, bitRate, quality);
        this.outputStream = outputStream;
    }

    private static native long createEncoder0(int channels, int sampleRate, int bitRate, int quality) throws IOException;

    private native byte[] writeInternal0(short[] input) throws IOException;

    /**
     * Writes the given samples to the output stream.
     *
     * @param input the samples to write
     * @throws IOException if an I/O error occurs
     */
    public void write(short[] input) throws IOException {
        synchronized (this) {
            byte[] buffer = writeInternal0(input);
            outputStream.write(buffer, 0, buffer.length);
        }
    }

    private native byte[] flush0() throws IOException;

    private native void destroyEncoder0();

    /**
     * Finalizes the mp3 file and closes the output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        synchronized (this) {
            byte[] flushBuffer = flush0();

            outputStream.write(flushBuffer, 0, flushBuffer.length);

            destroyEncoder0();
            lame = 0L;
            outputStream.close();
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return lame == 0L;
        }
    }

}
