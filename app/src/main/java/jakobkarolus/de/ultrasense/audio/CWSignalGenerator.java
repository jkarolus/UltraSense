package jakobkarolus.de.ultrasense.audio;

import android.util.FloatMath;

/**
 * Generate a continous wave signal of given frequency, amplitude and length (seconds)
 *<br><br>
 * Created by Jakob on 27.05.2015.
 */
public class CWSignalGenerator implements SignalGenerator{

    private double freq;
    private double length;
    private double amplitude;
    private double sampleRate;

    /**
     * creates a new CWSignalGenerator to be used in the AudioManager
     *
     * @param freq the frequency in Hz
     * @param length length of the signal in seconds
     * @param amplitude maximum amplitude
     * @param sampleRate the sample rate of the signal
     */
    public CWSignalGenerator(double freq, double length, double amplitude, double sampleRate) {
        this.freq = freq;
        this.length = length;
        this.amplitude = amplitude;
        this.sampleRate = sampleRate;
    }

    /**
     * creates a new CWSignalGenerator to be used in the AudioManager with default parameters
     *
     * @param freq the frequency in Hz
     * @param sampleRate the sample rate of the signal
     */
    public CWSignalGenerator(double freq, double sampleRate) {
        this.freq = freq;
        this.length = 0.5;
        this.amplitude = 1.0;
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
