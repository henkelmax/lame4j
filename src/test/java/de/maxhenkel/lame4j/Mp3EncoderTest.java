package de.maxhenkel.lame4j;

import de.maxhenkel.nativeutils.UnknownPlatformException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Mp3EncoderTest {

    @Test
    @DisplayName("Encode")
    void encode() throws IOException, UnknownPlatformException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Mp3Encoder encoder = new Mp3Encoder(1, 48000, 128, 5, out);
        for (int i = 0; i < 10; i++) {
            encoder.write(new short[960]);
        }
        encoder.close();

        assertTrue(out.toByteArray().length > 0);
    }

}
