package jakobkarolus.de.ultrasense.algorithm;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Utility class providing signal processing functionality.
 *
 * <br><br>
 * Created by Jakob on 14.05.2015.
 */
public class AlgoHelper {

    //butterworth filter
    private static double[] b = {0.010312874762664, -0.061877248575986,  0.154693121439966, -0.206257495253288, 0.154693121439966, -0.061877248575986,  0.010312874762664};
    private static double[] a = {1.000000000000000,  1.187600680175615,  1.305213349288551,  0.674327525297999, .263469348280139,  0.051753033879642,  0.005022526595088};


    /**
     * scales all data between -1 and 1
     *
     * @param data the data to scale
     * @return scaled data
     */
    public static double[] scaleToOne(double[] data) {
        double max = Double.MIN_VALUE;
        for(double d : data) {
            if (d > max)
                max = d;
        }
        for(int i=0; i < data.length; i++)
            data[i] /= max;

        return data;
    }

    /**
     * modulate the given data
     *
     * @param frequency the carrier frequency to use
     * @param sampleRate of the data
     * @param data the data
     * @return modulated signal
     */
    public static double[] modulate(double frequency, double sampleRate, double[] data){
        double[] carrierSignal = AlgoHelper.generateSignal(frequency, data.length, 1.0, sampleRate);
        for(int i=0; i < data.length; i++)
            data[i] *= carrierSignal[i];
        return data;
    }

    /**
     * downsamples the data by the given factor
     *
     * @param factor downsample factor
     * @param data the data
     */
    public static double[] downsample(int factor, double[] data){
        double[] downsampledData = new double[(int) Math.ceil(data.length/factor)];
        int counter=0;
        for(int i=0; i < data.length; i+=factor) {
            downsampledData[counter] = data[i];
            counter++;
        }
        return downsampledData;
    }


    /**
     * applies a 6th-order butterworth highpass filter with cutoff at 0.6
     * @param data
     * @return highpass filtered data
     */
    public static double[] applyHighPassFilter(double[] data){

        //http://dsp.stackexchange.com/questions/592/how-does-matlab-handle-iir-filters

        double xmem1, xmem2, ymem1, ymem2, xmem3, xmem4, ymem3, ymem4, xmem5, xmem6, ymem5, ymem6;
        xmem1 = xmem2 = ymem1 = ymem2 = xmem3 = xmem4 = ymem3 = ymem4 = xmem5 = xmem6 = ymem5 = ymem6 =0.0;


        int p=0;

        while(p < data.length){

            double y = b[0]*data[p] + b[1]*xmem1 + b[2]*xmem2 + b[3]*xmem3 + b[4]*xmem4 + b[5]*xmem5 + b[6]*xmem6
                    - a[1]*ymem1 - a[2]*ymem2 - a[3]*ymem3 - a[4]*ymem4 - a[5]*ymem5 - a[6]*ymem6;

            if(Double.isNaN(y))
                y = 0.0;

            xmem6 = xmem5;
            xmem5 = xmem4;
            xmem4 = xmem3;
            xmem3 = xmem2;
            xmem2 = xmem1;
            xmem1 = data[p];
            ymem6 = ymem5;
            ymem5 = ymem4;
            ymem4 = ymem3;
            ymem3 = ymem2;
            ymem2 = ymem1;
            ymem1 = y;

            data[p] = y;

            p++;
        }
        return data;

    }


    /**
     * applies fft processing to the given data
     * @param x the data (size must be power of 2)
     * @param win the window to use
     * @param windowAmp and its amplification to consider
     * @return the magnitude per frequency bin
     */
    public static double[] fftMagnitude(double[] x, double[] win, double windowAmp){
        for(int i=0; i < x.length; i++)
            x[i] = x[i] * win[i];

        FastFourierTransformer trans = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] result = trans.transform(x, TransformType.FORWARD);
        double[] magnitudeColumn = new double[x.length/2+1];
        int rown = (x.length/2+1);
        for(int i=0; i < rown; i++){
            double magnitude = result[i].getReal()*result[i].getReal() + result[i].getImaginary()*result[i].getImaginary();
            //adjust for window ampflication
            //magnitude = result[i].abs();
            //TODO: consider using 10*log10 instead (no need for sqrt)
            //-> changes the constant window amp factor -> new thresholding necessary
            magnitude = Math.sqrt(magnitude);
            magnitude = (magnitude/((double) x.length))/windowAmp;
            //magnitude /= windowAmp;
            //TODO adjust for nycquist or DC component necessary?
            if(!(i==0 || i==(rown-1)))
                magnitude *=2;

            //log scale
            magnitude = 20*Math.log10(magnitude+1e-6);
            magnitudeColumn[i] = magnitude;
        }

        return magnitudeColumn;

    }

    /**
     * norm for the given window (sum(win)/length(win))
     * @param win the window
     * @return the norm
     */
    public static double sumWindowNorm(double[] win) {
        double sum=0.0;
        for(int i=0; i < win.length; i++)
            sum+= win[i];
        return sum/((double) win.length);
    }

    /**
     * conmputes hann window for given size
     *
     * @param n the length of the window
     * @return a Hann window as double[]
     */
    public static double[] getHannWindow(int n){

        double[] hann = new double[n];
        for(int i=0; i < n; i++){
            hann[i] = 0.5*(1-Math.cos((2.0*Math.PI*i)/(n-1)));
        }
        return hann;

    }


    /**
     * generate a CW signal with the given properties
     * @param frequency the frequency
     * @param length sample length
     * @param amplitude maximum amplitude
     * @param sampleRate sample rate to use
     * @return data as double[]
     */
    public static double[] generateSignal(double frequency, int length, double amplitude, double sampleRate){

        double[] signal = new double[length];

        for(int i=0; i< length; i++){
            signal[i] = amplitude * Math.sin(frequency*2.0*Math.PI*(i/sampleRate));
        }

        return signal;
    }

    /**
     * performs cross-correlation via FFT.<br>
     * <b>CAUTION:</b> Can run out of memory for large sequences!
     *
     * @param signal1
     * @param signal2
     * @return cross-correlation as double[]
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

}
