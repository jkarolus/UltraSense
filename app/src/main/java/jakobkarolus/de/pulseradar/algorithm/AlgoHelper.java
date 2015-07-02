package jakobkarolus.de.pulseradar.algorithm;

import android.util.FloatMath;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

/**
 * Created by Jakob on 14.05.2015.
 */
public class AlgoHelper {


    public static double[] fftMagnitude(double[] x){
        double[] win = getHannWindow(x.length);
        for(int i=0; i < x.length; i++)
            x[i] *= win[i];

        FastFourierTransformer trans = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] result = trans.transform(x, TransformType.FORWARD);
        double[] magnitudeColumn = new double[x.length/2+1];
        for(int i=0; i < (x.length/2+1); i++){
            double magnitude = result[i].getReal()*result[i].getReal() + result[i].getImaginary()*result[i].getImaginary();
            //adjust for window ampflication
            magnitude /= x.length;
            magnitude /= sumWindowNorm(win);
            //TODO adjust for nycquist or DC component necessary?

            //log scale
            magnitude = 20*Math.log10(magnitude+1e-6);
            magnitudeColumn[i] = magnitude;
        }

        return magnitudeColumn;

    }

    private static double sumWindowNorm(double[] win) {
        double sum=0.0;
        for(int i=0; i < win.length; i++)
            sum+= win[i];
        return sum/((double) win.length);
    }

    /**
     *
     *
     * @param n the lenght of the window
     * @return a Hann window as double[]
     */
    public static double[] getHannWindow(int n){

        double[] hann = new double[n];
        for(int i=0; i < n; i++){
            hann[i] = 0.5*(1-Math.cos((2.0*Math.PI*i)/(n-1)));
        }
        return hann;

    }

    public static double[] generateSignal(double frequency, int length, double amplitude, double sampleRate){

        double[] signal = new double[length];

        for(int i=0; i< length; i++){
            signal[i] = amplitude * Math.sin(frequency*2.0*Math.PI*(i/sampleRate));
        }

        return signal;
    }

    public static double[] generateFMSignal(double topFreq, double bottomFreq, double chirpDuration, double chirpCycles, double sampleRate, float amplitude, boolean onlyRampUp){
        int singleChirpSamples = (int) (chirpDuration * sampleRate);
        double[] buffer = new double[(int) (singleChirpSamples * chirpCycles)];

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
        return buffer;
    }

    /**
     * performs cross-correlation via FFT
     *
     * @param signal1
     * @param signal2
     * @return
     */
    public static double[] xcorr(double[] signal1, double[] signal2){
        int corrLength = Math.max(signal1.length, signal2.length);
        int sizeFFT = nextPow2(2*corrLength);
        double[] signal1Padded = new double[sizeFFT];
        double[] signal2Padded  = new double[sizeFFT];
        System.arraycopy(signal1, 0, signal1Padded, 0, signal1.length);
        System.arraycopy(signal2, 0, signal2Padded, 0, signal2.length);

        FastFourierTransformer trans = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] result1 = trans.transform(signal1Padded, TransformType.FORWARD);
        Complex[] result2 = trans.transform(signal2Padded, TransformType.FORWARD);

        Complex[] result = new Complex[result1.length];
        for(int i=0; i < result.length; i++){
            result[i] = result1[i].multiply(result2[i].conjugate());
        }

        //clean up memory
        result1 = null;
        result2 = null;

        Complex[] corr = trans.transform(result, TransformType.INVERSE);
        result = null;
        double xcorr[] = new double[corr.length];

        for(int i=0; i < corr.length; i++)
            xcorr[i] = corr[i].getReal();

        corr = null;

        //shift symmetric and omitt excess zeros needed during FFT
        double xcorrShifted[] = new double[2*corrLength-1];
        int diff = sizeFFT - 2*corrLength;

        //the right half (becomes the negative side), excluding the zero component
        int rightHalf = xcorr.length-(diff+corrLength+1);

        //move the left side (positive part) and right side (negative part) to correct position
        System.arraycopy(xcorr, 0, xcorrShifted, rightHalf, corrLength);
        System.arraycopy(xcorr, (diff+corrLength+1), xcorrShifted, 0, rightHalf);


        return xcorrShifted;

    }

    private static int nextPow2(int x){
        if (x < 0)
            return 0;
        --x;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x+1;
    }


    //from: https://www.ee.columbia.edu/~ronw/code/dev/MEAPsoft/src/com/meapsoft/DSP.java
    /**
     * Convolves sequences a and b.  The resulting convolution has
     * length a.length+b.length-1.
     */
    public static double[] conv(double[] a, double[] b)
    {
        double[] y = new double[a.length+b.length-1];

        // make sure that a is the shorter sequence
        if(a.length > b.length)
        {
            double[] tmp = a;
            a = b;
            b = tmp;
        }

        for(int lag = 0; lag < y.length; lag++)
        {
            y[lag] = 0;

            // where do the two signals overlap?
            int start = 0;
            // we can't go past the left end of (time reversed) a
            if(lag > a.length-1)
                start = lag-a.length+1;

            int end = lag;
            // we can't go past the right end of b
            if(end > b.length-1)
                end = b.length-1;

            //System.out.println("lag = " + lag +": "+ start+" to " + end);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  ai = " + (lag-n) + ", bi = " + n);
                y[lag] += b[n]*a[lag-n];
            }
        }

        return(y);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    public static double[] xcorr2(double[] a, double[] b)
    {
        int len = a.length;
        if(b.length > a.length)
            len = b.length;

        return xcorr2(a, b, len-1);

        // // reverse b in time
        // double[] brev = new double[b.length];
        // for(int x = 0; x < b.length; x++)
        //     brev[x] = b[b.length-x-1];
        //
        // return conv(a, brev);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     * maxlag is the maximum lag to
     */
    public static double[] xcorr2(double[] a, double[] b, int maxlag)
    {
        double[] y = new double[2*maxlag+1];
        Arrays.fill(y, 0);

        for(int lag = b.length-1, idx = maxlag-b.length+1;
            lag > -a.length; lag--, idx++)
        {
            if(idx < 0)
                continue;

            if(idx >= y.length)
                break;

            // where do the two signals overlap?
            int start = 0;
            // we can't start past the left end of b
            if(lag < 0)
            {
                //System.out.println("b");
                start = -lag;
            }

            int end = a.length-1;
            // we can't go past the right end of b
            if(end > b.length-lag-1)
            {
                end = b.length-lag-1;
                //System.out.println("a "+end);
            }

            //System.out.println("lag = " + lag +": "+ start+" to " + end+"   idx = "+idx);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  bi = " + (lag+n) + ", ai = " + n);
                y[idx] += a[n]*b[lag+n];
            }
            //System.out.println(y[idx]);
        }

        return(y);
    }

}
