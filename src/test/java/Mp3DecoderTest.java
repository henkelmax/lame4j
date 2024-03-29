import de.maxhenkel.lame4j.DecodedAudio;
import de.maxhenkel.lame4j.Mp3Decoder;
import de.maxhenkel.lame4j.Mp3Encoder;
import de.maxhenkel.lame4j.UnknownPlatformException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class Mp3DecoderTest {

    @Test
    @DisplayName("Decode")
    void encode() throws IOException, UnknownPlatformException {
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
