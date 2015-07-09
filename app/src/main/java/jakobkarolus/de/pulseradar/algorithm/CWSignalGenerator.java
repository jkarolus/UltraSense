package jakobkarolus.de.pulseradar.algorithm;

import android.util.FloatMath;

/**
 * Generate a continous wave signal of given frequency, amplitude and length (seconds)
 *
 * Created by Jakob on 27.05.2015.
 */
public class CWSignalGenerator implements SignalGenerator{

    private double freq;
    private double length;
    private double amplitude;
    private double sampleRate;

    /**
     *
     *
     * @param freq the frequency in Hz
     * @param length length of the signal in seconds
     * @param amplitude maximum amplitude
     * @param sampleRate
     */
    public CWSignalGenerator(double freq, double length, double amplitude, double sampleRate) {
        this.freq = freq;
        this.length = length;
        this.amplitude = amplitude;
        this.sampleRate = sampleRate;
    }

    @Override
    public byte[] generateAudio() {

        float[] buffer = new float[(int) (length * sampleRate)];


        for (int sample = 0; sample < buffer.length; sample++) {
            double time = sample / sampleRate;
            double angle = freq * 2.0 * Math.PI * time;
            //make sure we got precise calculations
            angle %= 2.0*Math.PI;
            buffer[sample] = (float) (amplitude * FloatMath.sin((float)angle));
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
        return freq;
    }
}
