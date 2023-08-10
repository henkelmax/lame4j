package de.maxhenkel.lame4j;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;

public class Mp3Decoder implements Audio, AutoCloseable {

    private long decoder;
    private final InputStream inputStream;

    public Mp3Decoder(InputStream inputStream) throws IOException, UnknownPlatformException {
        Lame.load();
        this.inputStream = inputStream;
        decoder = createDecoder0();
    }

    private static native long createDecoder0();

    private native short[] decodeNextFrame0(InputStream stream) throws IOException;

    /**
     * Decodes the next frame in the mp3 file and returns the decoded audio data as PCM samples.
     * If the header of the mp3 file is not yet parsed, this method will parse the header.
     * <br/>
     * <b>NOTE</b>: If the mp3 file is invalid, the first frame will immediately return <code>null</code>.
     *
     * @return the decoded audio data as PCM samples or <code>null</code> if the end of the mp3 file is reached
     * @throws IOException if an I/O error occurs
     */
    public short[] decodeNextFrame() throws IOException {
        synchronized (this) {
            return decodeNextFrame0(inputStream);
        }
    }

    private native boolean headerParsed0();

    /**
     * @return if the header of the mp3 file is parsed
     */
    public boolean headerParsed() {
        synchronized (this) {
            return headerParsed0();
        }
    }

    private native int getChannelCount0();

    /**
     * @return the number of channels of the decoded audio or -1 if the header of the mp3 file is not yet parsed
     */
    public int getChannelCount() {
        synchronized (this) {
            return getChannelCount0();
        }
    }

    private native int getBitRate0();

    /**
     * @return the bitrate of the mp3 file or -1 if the header of the mp3 file is not yet parsed
     */
    public int getBitRate() {
        synchronized (this) {
            return getBitRate0();
        }
    }

    private native int getSampleRate0();

    /**
     * @return the sample rate of the decoded audio or -1 if the header of the mp3 file is not yet parsed
     */
    public int getSampleRate() {
        synchronized (this) {
            return getSampleRate0();
        }
    }

    /**
     * Creates an AudioFormat object for the decoded audio.
     *
     * @return the audio format of the decoded audio or null if the header of the mp3 file is not yet parsed
     */
    @Override
    @Nullable
    public AudioFormat createAudioFormat() {
        if (!headerParsed()) {
            return null;
        }
        return Audio.super.createAudioFormat();
    }

    private native void destroyDecoder0();

    @Override
    public void close() throws IOException {
        synchronized (this) {
            destroyDecoder0();
            decoder = 0L;
            inputStream.close();
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return decoder == 0L;
        }
    }

    /**
     * Decodes the mp3 file and returns the decoded audio data.
     *
     * @param inputStream the input stream of the mp3 file
     * @return the decoded audio data as PCM samples
     * @throws IOException              if an I/O error occurs or the mp3 file is invalid
     * @throws UnknownPlatformException if the platform is not supported
     */
    public static DecodedAudio decode(InputStream inputStream) throws IOException, UnknownPlatformException {
        try (Mp3Decoder decoder = new Mp3Decoder(inputStream)) {
            ShortArrayBuffer sampleBuffer = new ShortArrayBuffer(2048);
            while (true) {
                short[] samples = decoder.decodeNextFrame();
                if (samples == null) {
                    if (sampleBuffer.size() <= 0) {
                        throw new IOException("No audio data found");
                    }
                    break;
                }
                sampleBuffer.writeShorts(samples);
            }
            if (!decoder.headerParsed()) {
                throw new IOException("No header found");
            }
            return new DecodedAudio(decoder.getChannelCount(), decoder.getSampleRate(), decoder.getBitRate(), sampleBuffer.toShortArray());
        }
    }

}
