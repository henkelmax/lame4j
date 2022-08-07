package de.maxhenkel.lame4j;

import com.sun.jna.*;

import java.util.Arrays;
import java.util.List;

public interface Lame extends Library {

    Lame INSTANCE = Native.loadLibrary(NativeLibrary.getInstance(LibraryLoader.getPath()).getFile().getAbsolutePath(), Lame.class);

    Pointer lame_init();

    int lame_set_num_channels(Pointer gfp, int channels);

    int lame_set_in_samplerate(Pointer gfp, int sampleRate);

    int lame_set_brate(Pointer gfp, int bitRate);

    int lame_set_mode(Pointer gfp, int mode);

    int lame_set_quality(Pointer gfp, int quality);

    int lame_init_params(Pointer gfp);

    int lame_encode_buffer(Pointer gfp, short[] leftPcm, short[] rightPcm, int numSamples, byte[] mp3buffer, int mp3bufferSize);

    int lame_encode_buffer_interleaved(Pointer gfp, short[] pcm, int numSamples, byte[] mp3buffer, int mp3bufferSize);

    int lame_encode_flush(Pointer gfp, byte[] mp3buffer, int mp3bufferSize);

    int lame_close(Pointer gfp);

    int lame_set_decode_only(Pointer gfp, int decodeOnly);

    Pointer hip_decode_init();

    int hip_decode_headers(Pointer hip, byte[] mp3buffer, int mp3bufferSize, short[] pcmLeft, short[] pcmRight, Mp3Data mp3Data);

    int hip_decode1_headers(Pointer hip, byte[] mp3buffer, int mp3bufferSize, short[] pcmLeft, short[] pcmRight, Mp3Data mp3Data);

    int hip_decode1_headersB(Pointer hip, byte[] mp3buffer, int mp3bufferSize, short[] pcmLeft, short[] pcmRight, Mp3Data mp3Data, Pointer encDelay, Pointer encPadding);

    int hip_decode(Pointer hip, byte[] mp3buffer, int mp3bufferSize, short[] pcmLeft, short[] pcmRight);

    int hip_decode1(Pointer hip, byte[] mp3buffer, int mp3bufferSize, short[] pcmLeft, short[] pcmRight);

    int hip_decode_exit(Pointer hip);

    public static class LameMode {
        public static final int STEREO = 0;
        public static final int JOINT_STEREO = 1;
        public static final int DUAL_CHANNEL = 2;
        public static final int MONO = 3;
        public static final int NOT_SET = 4;
        public static final int MAX_INDICATOR = 5;
    }

    public class Mp3Data extends Structure {
        public int header_parsed;
        public int stereo;
        public int samplerate;
        public int bitrate;
        public int mode;
        public int mode_ext;
        public int framesize;
        public long nsamp;
        public int totalframes;
        public int framenum;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("header_parsed", "stereo", "samplerate", "bitrate", "mode", "mode_ext", "framesize", "nsamp", "totalframes", "framenum");
        }
    }

}
