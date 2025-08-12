package de.maxhenkel.lame4j;

import de.maxhenkel.nativeutils.UnknownPlatformException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class Mp3DecoderTest {

    @Test
    @DisplayName("Create decoder")
    void createDecoder() throws IOException, UnknownPlatformException {
        new Mp3Decoder(new ByteArrayInputStream(new byte[0])).close();
    }

    @Test
    @DisplayName("Decode")
    void decode() throws IOException, UnknownPlatformException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mp3Encoder encoder = new Mp3Encoder(1, 48000, 128, 5, out);
        for (int i = 0; i < 10; i++) {
            encoder.write(new short[960]);
        }
        encoder.close();

        DecodedAudio decoded = Mp3Decoder.decode(new ByteArrayInputStream(out.toByteArray()));

        assertTrue(decoded.getSamples().length > 0);
        assertEquals(48000, decoded.getSampleRate());
        assertEquals(128, decoded.getBitRate());
        assertEquals(1, decoded.getChannelCount());
    }

}
