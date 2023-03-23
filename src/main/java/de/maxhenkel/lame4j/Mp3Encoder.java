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
        this.lame = createEncoder(channels, sampleRate, bitRate, quality);
        this.outputStream = outputStream;
    }

    private static native long createEncoder(int channels, int sampleRate, int bitRate, int quality) throws IOException;

    private native byte[] writeInternal(short[] input) throws IOException;

    /**
     * Writes the given samples to the output stream.
     *
     * @param input the samples to write
     * @throws IOException if an I/O error occurs
     */
    public void write(short[] input) throws IOException {
        byte[] buffer = writeInternal(input);
        outputStream.write(buffer, 0, buffer.length);
    }

    private native byte[] flush() throws IOException;

    private native void destroyEncoder();

    /**
     * Finalizes the mp3 file and closes the output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        byte[] flushBuffer = flush();

        outputStream.write(flushBuffer, 0, flushBuffer.length);

        destroyEncoder();
        lame = 0L;
        outputStream.close();
    }

    public boolean isClosed() {
        return lame == 0L;
    }

}
