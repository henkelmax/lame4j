package de.maxhenkel.lame4j;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InputOutputTest {

    @Test
    void testInputOutput() throws Exception {
        double[] frequencies = {440D, 554.37D, 659.25D};
        int sampleRate = 44100;
        short[] shorts = TestUtils.generateAudio(frequencies, sampleRate, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Mp3Encoder enc = new Mp3Encoder(1, sampleRate, 128, 0, baos)) {
            enc.write(shorts);
        }
        byte[] mp3 = baos.toByteArray();
        DecodedAudio decoded = Mp3Decoder.decode(new ByteArrayInputStream(mp3));
        float similarity = TestUtils.pcmSimilarity(shorts, decoded.getSamples());
        assertEquals(1F, similarity, 0.04F);
    }

    @Test
    void testInputOutputNotSimilar() throws Exception {
        double[] frequencies1 = {440D, 554.37D, 659.25D};
        double[] frequencies2 = {392D, 493.88D, 587.33D};
        int sampleRate = 44100;
        short[] shorts1 = TestUtils.generateAudio(frequencies1, sampleRate, 3);
        short[] shorts2 = TestUtils.generateAudio(frequencies2, sampleRate, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Mp3Encoder enc = new Mp3Encoder(1, sampleRate, 128, 0, baos)) {
            enc.write(shorts1);
        }
        byte[] mp3 = baos.toByteArray();
        DecodedAudio decoded = Mp3Decoder.decode(new ByteArrayInputStream(mp3));
        float similarity = TestUtils.pcmSimilarity(shorts2, decoded.getSamples());
        assertEquals(0F, similarity, 0.7F);
    }

    @Test
    void testInputOutputSilent() throws Exception {
        double[] frequencies = {100D};
        int sampleRate = 44100;
        short[] sound = TestUtils.generateAudio(frequencies, sampleRate, 3);
        short[] silence = new short[sound.length];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Mp3Encoder enc = new Mp3Encoder(1, sampleRate, 128, 0, baos)) {
            enc.write(sound);
        }
        byte[] mp3 = baos.toByteArray();
        DecodedAudio decoded = Mp3Decoder.decode(new ByteArrayInputStream(mp3));
        float similarity = TestUtils.pcmSimilarity(silence, decoded.getSamples());
        assertEquals(0F, similarity, 0.6F);
    }

}
