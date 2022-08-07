package de.maxhenkel.lame4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CliConverter {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java -jar lame4j.jar <input file> <output file>");
            return;
        }
        LameDecoder decoder = new LameDecoder(Files.newInputStream(Paths.get(args[0])));

        short[] decode = decoder.decode();

        System.out.println("Sample Rate: " + decoder.getSampleRate());
        System.out.println("Bitrate: " + decoder.getBitrate());
        System.out.println("Channels: " + decoder.getChannelCount());
        System.out.println("FrameSize: " + decoder.getFrameSize());

        System.out.println("Length: " + decode.length + " samples");
        System.out.println("Duration: " + ((float) decode.length / (float) decoder.getSampleRate()) + " seconds");

        LameEncoder encoder = new LameEncoder(decoder.getChannelCount(), decoder.getSampleRate(), decoder.getBitrate(), 5, Files.newOutputStream(Paths.get(args[1])));
        encoder.write(decode);
        encoder.close();
    }

}
