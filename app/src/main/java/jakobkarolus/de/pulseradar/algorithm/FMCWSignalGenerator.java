package jakobkarolus.de.pulseradar.algorithm;

import android.util.FloatMath;

/**
 * Created by Jakob on 27.05.2015.
 */
public class FMCWSignalGenerator implements SignalGenerator{


    private double topFreq;
    private double bottomFreq;
    private double chirpDuration;
    private double chirpCycles;
    private double sampleRate;
    private float amplitude;
    private boolean onlyRampUp;

    public FMCWSignalGenerator(double topFreq, double bottomFreq, double chirpDuration, double chirpCycles, double sampleRate, float amplitude, boolean onlyRampUp) {
        this.topFreq = topFreq;
        this.bottomFreq = bottomFreq;
        this.chirpDuration = chirpDuration;
        this.chirpCycles = chirpCycles;
        this.sampleRate = sampleRate;
        this.amplitude = amplitude;
        this.onlyRampUp = onlyRampUp;
    }

    @Override
    public byte[] generateAudio() {


        int singleChirpSamples = (int) (chirpDuration * sampleRate);
        float[] buffer = new float[(int) (singleChirpSamples * chirpCycles)];

        double freqIncline = (topFreq - bottomFreq)/(chirpDuration);
        double frequency = bottomFreq;

        for(int cycle=0; cycle < chirpCycles; cycle++){

            for (int sample = 0; sample < singleChirpSamples; sample++) {

                double time = (double)sample/sampleRate;
                double phase = 2.0*Math.PI*time*(frequency + (freqIncline/2.0)*time);

                buffer[cycle*singleChirpSamples + sample]  = amplitude* FloatMath.sin((float) phase);

            }
            if(!onlyRampUp) {
                freqIncline *= -1;
                if(Math.abs(frequency - bottomFreq) < 1e-6)
                    frequency = topFreq;
                else
                    frequency = bottomFreq;
            }
        }


        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        return byteBuffer;

    }

    @Override
    public double getCarrierFrequency() {
        return bottomFreq;
    }
}
