package jakobkarolus.de.ultrasense.algorithm;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.Serializable;

/**
 * Implements a Short time Fourier transforms<br>
 * Based on StftManager in Matlab from M.Sc. Eng. Hristo Zhivomirov 12/21/13<br><br>
 * Keeps track of current STFT
 * Created by Jakob on 14.05.2015.
 */
public class StftManager implements Serializable{

    private static final double SAMPLE_RATE = 44100;
    private static final int WINDOW_LENGTH = 4096;
    private static final int HOP_SIZE = 1024;
    private static final int NFFT = 4096;

    private double sampleRate;
    private int windowLength;
    private int nfft;
    private int hopSize;
    private double[] window;
    private double[] data;

    private double[][] currentSTFT;


    /**
     * specify custom fft parameters
     * @param nfft number of fft points
     * @param hopSize hopSize of the window
     * @param window window function
     * @param sampleRate the sample rate of the signal
     */
    public StftManager(int nfft, int hopSize, double[] window, double sampleRate) {
        this.windowLength = window.length;
        this.nfft = nfft;
        this.hopSize = hopSize;
        this.window = window;
        this.sampleRate = sampleRate;

    }

    /**
     * uses std values for fft paras
     */
    public StftManager() {
        this.windowLength = WINDOW_LENGTH;
        this.nfft = NFFT;
        this.hopSize = HOP_SIZE;
        this.window = AlgoHelper.getHannWindow(WINDOW_LENGTH);
        this.sampleRate = SAMPLE_RATE;

    }

    /**
     * applies a 6th-order butterworth highpass filter (cutoff: 0.6) to the data
     */
    public void applyHighPassFilter(){
        if(this.data != null)
            this.data = AlgoHelper.applyHighPassFilter(data);

    }


    /**
     * modulate the data with a signal of the specified frequency
     * @param frequency the carrier frequency
     */
    public void modulate(double frequency){
        if(this.data != null)
            this.data = AlgoHelper.modulate(frequency, sampleRate, this.data);
    }

    /**
     * downsamples the data by the given factor
     *
     * @param factor
     */
    public void downsample(int factor){
        if(this.data != null)
            this.data = AlgoHelper.downsample(factor, this.data);
    }


    /**
     * computes a STFT as a 2D grid of frequency-bin vs timestep, where time increases along a row, while frequency along a column.
     * In other words: stft[0] contains magnitude values of all frequency-bins for the first timestep.<br>
     *
     * can be accessed via getCurrentSTFT()
     */
    public void computeSTFT(){

        int dataLength = data.length;
        int rown = (int) Math.ceil((1+nfft)/2.0);
        int coln = (int) (1+ Math.floor((dataLength-windowLength)/(double)hopSize));
        double[][] stft = new double[coln][rown];

        double windowAmp = getCoherentWindowAmplification(window);

        int indx = 0;
        int col = 0;

        while((indx + windowLength) <= dataLength){
            double[] xw = getPartialSignal(indx, data, windowLength, window);
            FastFourierTransformer trans = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] result = trans.transform(xw, TransformType.FORWARD);
            //double[] magnitude = new double[result.length];
            //for(int i =0 ; i < result.length; i++){
                //magnitude[i] = result[i].getReal()*result[i].getReal() + result[i].getImaginary()*result[i].getImaginary();
            //}

            //stft[col] = getSingleSideSpectrum(magnitude, rown);
            for(int i=0; i < rown;i++) {
                double entry = result[i].getReal()*result[i].getReal() + result[i].getImaginary()*result[i].getImaginary();
                entry = (entry/((double) windowLength))/windowAmp;

                if(i != 0) {
                    if(i != (rown-1) || (i == (rown-1)) && (i%2==1))
                        entry *= 2;
                }

                stft[col][i] = 20 * Math.log10(entry + 1e-6);
            }
            indx +=hopSize;
            col++;

        }

        currentSTFT = stft;
    }

    private double getCoherentWindowAmplification(double[] window) {
        double sum = 0.0;
        for(int i=0; i < window.length;i++)
            sum+=window[i];
        return sum/((double) window.length);
    }

    private double[] getPartialSignal(int idx, double[] buffer, int wlen, double[] window) {
        double[] xw = new double[wlen];
        System.arraycopy(buffer, idx, xw, 0, wlen);
        for(int i=0; i < wlen; i++)
            xw[i] = xw[i]*window[i];
        return xw;
    }

    private double[] getSingleSideSpectrum(double[] magnitude, int n) {
        double[] singleSide = new double[n];
        System.arraycopy(magnitude, 0, singleSide, 0, n);
        return singleSide;
    }

    /**
     * return the current data; content depends on what methods have been called on it
     *
     * @return the current data
     */
    public double[] getData() {
        return data;
    }

    /**
     * data will get rescaled between -1 and 1
     *
     * @param data the data to perform STFT on
     */
    public void setData(double[] data) {
        this.data = AlgoHelper.scaleToOne(data);
    }

    public double[][] getCurrentSTFT(){
        return this.currentSTFT;
    }
}
