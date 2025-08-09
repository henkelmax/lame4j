package de.maxhenkel.lame4j;

public class TestUtils {

    public static short[] generateAudio(double[] frequencies, int sampleRate, double seconds) {
        int n = (int) Math.round(seconds * sampleRate);
        double attack = 0.02D; // 20 ms fade-in
        double release = 0.02D; // 20 ms fade-out
        int aSamp = (int) (attack * sampleRate);
        int rSamp = (int) (release * sampleRate);

        short[] out = new short[n];
        double perVoiceAmp = 0.6D / frequencies.length;
        double twoPiOverSr = 2D * Math.PI / sampleRate;

        for (int i = 0; i < n; i++) {
            double env = 1D;
            if (i < aSamp) env = i / (double) aSamp;
            else if (i > n - rSamp) env = (n - i) / (double) rSamp;

            double sample = 0D;
            for (double f : frequencies) {
                sample += perVoiceAmp * Math.sin(twoPiOverSr * f * i);
            }
            sample *= env;

            if (sample > 1D) sample = 1D;
            if (sample < -1D) sample = -1D;
            out[i] = (short) Math.round(sample * Short.MAX_VALUE);
        }
        return out;
    }

    public static float pcmSimilarity(short[] a, short[] b) {
        return pcmSimilarity(a, b, 4096);
    }

    public static float pcmSimilarity(short[] a, short[] b, int maxLag) {
        if (a.length == 0 && b.length == 0) {
            return 1F;
        }
        if (a.length == 0 || b.length == 0) {
            return 0F;
        }

        int lenA = a.length, lenB = b.length;
        float best = 0F;

        for (int lag = -maxLag; lag <= maxLag; lag++) {
            int startA = Math.max(0, -lag);
            int startB = Math.max(0, lag);
            int overlap = Math.min(lenA - startA, lenB - startB);
            if (overlap <= 0) continue;

            long sumSq = 0L;
            for (int i = 0; i < overlap; i++) {
                int diff = a[startA + i] - b[startB + i];
                sumSq += (long) diff * (long) diff;
            }

            double mse = (double) sumSq / overlap;
            double rmse = Math.sqrt(mse);
            double fullScale = 32767D;
            double overlapFactor = (double) overlap / Math.max(lenA, lenB);
            double score = Math.max(0D, 1D - (rmse / fullScale)) * overlapFactor;

            if (score > best) best = (float) score;
        }
        return best;
    }

}
