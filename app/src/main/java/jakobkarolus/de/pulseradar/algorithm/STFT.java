package jakobkarolus.de.pulseradar.algorithm;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Implements a Short time Fourier transforms
 * Created by Jakob on 14.05.2015.
 */
public class STFT {

    private int windowLength;
    private int nfft;
    private int hopSize;
    private double[] window;


    public STFT(int windowLength, int nfft, int hopSize, double[] window) {
        this.windowLength = windowLength;
        this.nfft = nfft;
        this.hopSize = hopSize;
        this.window = window;
    }

    /**
     * returns a 2D grid of frequency-bin vs timestep, where time increases along a row, while frequency along a column.
     * In other words: stft[0] contains magnitude values of all frequency-bins for the first timestep.
     *
     * @param data the real valued data to transform
     * @return 2D double array containing magnitude of each frequency-bin per timestep
     */
    public double[][] computeSTFT(double[] data){

        int dataLength = data.length;
        int rown = (int) Math.ceil((1+nfft)/2.0);
        int coln = (int) (1+ Math.floor((dataLength-windowLength)/(double)hopSize));
        double[][] stft = new double[coln][rown];

        int indx = 0;
        int col = 0;

        while((indx + windowLength) <= dataLength){
            double[] xw = getPartialSignal(indx, data, windowLength, window);
            FastFourierTransformer trans = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] result = trans.transform(xw, TransformType.FORWARD);
            double[] magnitude = new double[result.length];
            for(int i =0 ; i < result.length; i++){
                magnitude[i] = result[i].getReal()*result[i].getReal() + result[i].getImaginary()*result[i].getImaginary();
            }

            stft[col] = getSingleSideSpectrum(magnitude, rown);
            indx +=hopSize;
            col++;

        }

        return stft;
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
}
