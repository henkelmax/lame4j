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
        decoder = createDecoder();
    }

    private static native long createDecoder();

    /**
     * Decodes the next frame in the mp3 file and returns the decoded audio data as PCM samples.
     * If the header of the mp3 file is not yet parsed, this method will parse the header.
     *
     * @return the decoded audio data as PCM samples
     * @throws IOException if an I/O error occurs
     */
    private native short[] decodeNextFrame(InputStream stream) throws IOException;

    public short[] decodeNextFrame() throws IOException {
        return decodeNextFrame(inputStream);
    }

    /**
     * @return if the header of the mp3 file is parsed
     */
    public native boolean headerParsed();

    /**
     * @return the number of channels of the decoded audio or -1 if the header of the mp3 file is not yet parsed
     */
    public native int getChannelCount();

    /**
     * @return the bitrate of the mp3 file or -1 if the header of the mp3 file is not yet parsed
     */
    public native int getBitRate();

    /**
     * @return the sample rate of the decoded audio or -1 if the header of the mp3 file is not yet parsed
     */
    public native int getSampleRate();

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

    private native void destroyDecoder();

    @Override
    public void close() throws IOException {
        destroyDecoder();
        decoder = 0L;
        inputStream.close();
    }

    public boolean isClosed() {
        return decoder == 0L;
    }

    public static DecodedAudio decode(InputStream inputStream) throws IOException, UnknownPlatformException {
        try (Mp3Decoder decoder = new Mp3Decoder(inputStream)) {
            ShortArrayBuffer sampleBuffer = new ShortArrayBuffer(2048);
            while (true) {
                short[] samples = decoder.decodeNextFrame();
                if (samples == null) {
                    break;
                }
                sampleBuffer.writeShorts(samples);
            }
            return new DecodedAudio(decoder.getChannelCount(), decoder.getSampleRate(), decoder.getBitRate(), sampleBuffer.toShortArray());
        }
    }

}
