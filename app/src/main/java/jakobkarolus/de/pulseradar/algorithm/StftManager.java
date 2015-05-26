package jakobkarolus.de.pulseradar.algorithm;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Implements a Short time Fourier transforms<br>
 * Based on StftManager in Matlab from M.Sc. Eng. Hristo Zhivomirov 12/21/13<br><br>
 * Keeps track of current STFT
 * Created by Jakob on 14.05.2015.
 */
public class StftManager {

    private static final double SAMPLE_RATE = 44100;
    private static int WINDOW_LENGTH = 4096;
    private static int HOP_SIZE = 1024;
    private static int NFFT = 4096;

    private double sampleRate;
    private int windowLength;
    private int nfft;
    private int hopSize;
    private double[] window;
    private double[] data;

    private double[][] currentSTFT;

    //buttworth filter
    private static double[] b = {0.010312874762664, -0.061877248575986,  0.154693121439966, -0.206257495253288, 0.154693121439966, -0.061877248575986,  0.010312874762664};
    private static double[] a = {1.000000000000000,  1.187600680175615,  1.305213349288551,  0.674327525297999, .263469348280139,  0.051753033879642,  0.005022526595088};


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

    private double[] scaleToOne(double[] data) {
        double max = Double.MIN_VALUE;
        for(double d : data) {
            if (d > max)
                max = d;
        }
        for(int i=0; i < data.length; i++)
            data[i] /= max;
        return data;

    }

    public void applyHighPassFilterOld(){

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

    }

    public void applyHighPassFilter(){

        double[] xmem = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] ymem = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        int p=0;


        while(p < data.length){

            double y = b[0]*data[p];
            for(int i=0; i < xmem.length; i++){
                y+= b[i+1]*xmem[i];
                y-= a[i+1]*ymem[i];
            }

            for(int i=0; i < xmem.length-1; i++){
                xmem[i+1] = xmem[i];
                ymem[i+1] = ymem[i];
            }

            xmem[0] = data[p];
            ymem[0] = y;
            data[p] = y;

            p++;
        }

    }


    public void modulate(double frequency){
        double[] carrierSignal = AlgoHelper.generateSignal(frequency, data.length, 1.0, sampleRate);
        for(int i=0; i < data.length; i++)
            data[i] *= carrierSignal[i];
    }

    public void downsample(int factor){
        double[] downsampledData = new double[(int) Math.ceil(data.length/factor)];
        int counter=0;
        for(int i=0; i < data.length; i+=factor) {
            downsampledData[counter] = data[i];
            counter++;
        }
        data = downsampledData;
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

    public double[] getData() {
        return data;
    }

    /**
     * data will get rescaled between -1 and 1
     *
     * @param data the data to perform STFT on
     */
    public void setData(double[] data) {
        this.data = scaleToOne(data);
    }

    public double[][] getCurrentSTFT(){
        return this.currentSTFT;
    }
}
