package jakobkarolus.de.ultrasense.features;

import jakobkarolus.de.ultrasense.algorithm.AlgoHelper;

/**
 * FeatureDetector using a mean-based scheme per timestep to detect Features.<br>
 * Implements a STFT to get the power of frequency value per timestep
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class MeanBasedFD extends FeatureDetector{

    private int fftLength;
    private int hopSize;
    private double carrierIdx;
    private int halfCarrierWidth;
    private double magnitudeThreshold;
    private double featHighThreshold;
    private double featLowThreshold;
    private int featSlackWidth;
    private int currentSlack;
    private double[] win;
    private double windowAmp;
    private boolean ignoreNoise;
    private double maxFeatureThreshold;

    private double[] carryOver;
    private boolean carryAvailable;


    /**
     * creates a new MeanBasedFD with the given feature detection parameters
     *
     * @param featureProcessor the FeatureProcessor associated with this FeatureDetector
     * @param sampleRate sampleRate of the signal
     * @param fftLength fft length to use
     * @param hopSize hop size during fft processing
     * @param carrierFrequency carrier frequency
     * @param halfCarrierWidth single side magnitude extend (over lower and higher freq bins respectively) of the carrier frequency
     * @param magnitudeThreshold magnitude threshold to use
     * @param featHighThreshold threshold to overcome for starting a feature
     * @param featLowThreshold threshold to overcome for continuing an already started feature
     * @param featSlackWidth number of times the low thresholds is allowed to be bigger than the feature value to still continue the feature
     * @param win fft window to use
     * @param ignoreNoise whether to ignoreNoise
     * @param maxFeatureThreshold the maximum allowed feature value, only valid in conjunction with ignoreNoise==true
     */
    public MeanBasedFD(FeatureProcessor featureProcessor, double sampleRate, int fftLength, int hopSize, double carrierFrequency, int halfCarrierWidth, double magnitudeThreshold, double featHighThreshold, double featLowThreshold, int featSlackWidth, double[] win, boolean ignoreNoise, double maxFeatureThreshold) {
        super((double) hopSize / sampleRate, featureProcessor);
        this.fftLength = fftLength;
        this.hopSize = hopSize;
        this.carrierIdx = ((carrierFrequency/(sampleRate/2.0))*(fftLength/2+1))-1;
        this.halfCarrierWidth = halfCarrierWidth;
        this.magnitudeThreshold = magnitudeThreshold;
        this.featHighThreshold = featHighThreshold;
        this.featLowThreshold = featLowThreshold;
        this.featSlackWidth = featSlackWidth;
        this.currentSlack = 0;
        this.carryOver = new double[hopSize];
        this.win = win;
        this.windowAmp = AlgoHelper.sumWindowNorm(win);
        this.ignoreNoise = ignoreNoise;
        this.maxFeatureThreshold = maxFeatureThreshold;
        this.carryAvailable = false;
    }

    @Override
    public void checkForFeatures(double[] audioBuffer, boolean applyHighPass) {

        if(applyHighPass)
            AlgoHelper.applyHighPassFilter(audioBuffer);

        double[] tempBuffer;
        //buffer is assumed to be a multiple of 4096, plus added hopSize from the previous buffer
        if(carryAvailable) {
            tempBuffer = new double[audioBuffer.length + hopSize];
            System.arraycopy(carryOver, 0, tempBuffer, 0, hopSize);
            System.arraycopy(audioBuffer, 0, tempBuffer, hopSize, audioBuffer.length);
            //save the carry-over for the next buffer
            System.arraycopy(audioBuffer, audioBuffer.length - hopSize, carryOver, 0, hopSize);
        }
        else{
            tempBuffer = new double[audioBuffer.length];
            System.arraycopy(audioBuffer, 0, tempBuffer, 0, audioBuffer.length);
            //save the carry-over for the next buffer
            System.arraycopy(audioBuffer, audioBuffer.length - hopSize, carryOver, 0, hopSize);
            carryAvailable  = true;

        }

        double[] buffer = new double[fftLength];
        for(int i=0; i <= tempBuffer.length - fftLength; i+=hopSize){
            System.arraycopy(tempBuffer, i, buffer, 0, fftLength);

            increaseTime();
            double[] values = AlgoHelper.fftMagnitude(buffer, win, windowAmp);
            double[] valueForTimeStep = meanExtraction(values, carrierIdx, halfCarrierWidth);

            processFeatureValue(getCurrentHighFeature(), valueForTimeStep[0], true);
            processFeatureValue(getCurrentLowFeature(), valueForTimeStep[1], false);

        }

    }

    @Override
    public String printParameters() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("FD parameters:\n");
        buffer.append("thr: " + magnitudeThreshold + ", high: " + featHighThreshold + ", low: " + featLowThreshold + ", halfWidth: " + halfCarrierWidth);
        buffer.append(", slack: " + featSlackWidth + ", ignoreNoise: " + ignoreNoise + " (max: " + maxFeatureThreshold + ")");
        return buffer.toString();
    }

    private void processFeatureValue(UnrefinedFeature uF, double valueForTimeStep, boolean isHighDoppler) {
        if(valueForTimeStep >= featHighThreshold){
            if(!uF.hasStarted()){
                //start a new feature
                uF.setHasStarted(true);
                uF.setStartTime(getTime());
                currentSlack = 0;
                uF.addTimeStep(valueForTimeStep);
            }
            else{
                //reset slack
                currentSlack = 0;
                uF.addTimeStep(valueForTimeStep);
            }
        }
        else{
            if(uF.hasStarted()){
                //is it also below the low threshold
                if(valueForTimeStep < featLowThreshold){
                    //check for slack
                    if(currentSlack < featSlackWidth){
                        //ignore this one and go on
                        currentSlack++;
                        uF.addTimeStep(valueForTimeStep);
                    }
                    else{
                        //if it below low_threshold and no slack left -> finish it
                        //these means we have used all our slack to no avail -> remove them to get the correct feature size
                        if(uF.removeSlackedSteps(featSlackWidth)) {
                            uF.setHasStarted(false);
                            uF.setEndTime(getTime() - getTimeIncreasePerStep());
                            if(isHighDoppler)
                                notifyFeatureDetectedHigh();
                            else
                                notifyFeatureDetectedLow();
                        }
                    }
                }
                else{
                    //below threshold is fine for already started features
                    uF.addTimeStep(valueForTimeStep);
                }
            }
        }
    }

    private double[] meanExtraction(double[] values, double carrierIdx, int halfCarrierWidth) {
        double[] means = new double[2];

        //high doppler
        double meanWeightsHigh = 0.0;
        double meanHigh = 0.0;
        int offset = (int) Math.ceil(carrierIdx) + halfCarrierWidth;
        for(int i=0; i < (values.length - offset); i++){
            if(values[offset + i] > magnitudeThreshold){
                meanHigh += (i+1)*values[offset + i];
                meanWeightsHigh += values[offset + i];
            }
        }
        if(Math.abs(0.0 - meanWeightsHigh) > 1e-6) {
            meanHigh /= meanWeightsHigh;
        }
        //low doppler
        double meanWeightsLow = 0.0;
        double meanLow = 0.0;
        offset = (int) Math.floor(carrierIdx) - halfCarrierWidth;
        for(int i=0; i <= offset; i++){
            if(values[offset - i] > magnitudeThreshold){
                meanLow += (i+1)*values[offset - i];
                meanWeightsLow += values[offset - i];
            }
        }
        if(Math.abs(0.0 - meanWeightsLow) > 1e-6) {
            meanLow /= meanWeightsLow;
        }

        means[0] = meanHigh;
        means[1] = meanLow;

        if(ignoreNoise){
            for(int i=0; i< means.length; i++)
                if(means[i] > maxFeatureThreshold)
                    means[i] = 0.0;
        }

        return means;

    }
}
