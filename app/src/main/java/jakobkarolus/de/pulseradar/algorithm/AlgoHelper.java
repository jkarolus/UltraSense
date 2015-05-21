package jakobkarolus.de.pulseradar.algorithm;

/**
 * Created by Jakob on 14.05.2015.
 */
public class AlgoHelper {

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

}
