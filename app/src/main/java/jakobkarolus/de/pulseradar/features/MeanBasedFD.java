package jakobkarolus.de.pulseradar.features;

import jakobkarolus.de.pulseradar.algorithm.AlgoHelper;

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

    private double[] carryOver;
    private boolean carryAvailable;
    private long timeStep;

    public MeanBasedFD(int fftLength, int hopSize, double carrierFrequency, int halfCarrierWidth, double magnitudeThreshold, double featHighThreshold, double featLowThreshold, int featSlackWidth, double[] win) {
        super();
        this.fftLength = fftLength;
        this.hopSize = hopSize;
        this.carrierIdx = ((carrierFrequency/22050.0)*(fftLength/2+1))-1;
        this.halfCarrierWidth = halfCarrierWidth;
        this.magnitudeThreshold = magnitudeThreshold;
        this.featHighThreshold = featHighThreshold;
        this.featLowThreshold = featLowThreshold;
        this.featSlackWidth = featSlackWidth;
        this.currentSlack = 0;
        this.carryOver = new double[hopSize];
        this.win = win;
        this.windowAmp = AlgoHelper.sumWindowNorm(win);

        //instantiate new FD every time recording starts to reset carry
        this.carryAvailable = false;
        this.timeStep = 0;
    }

    @Override
    public void checkForFeatures(double[] audioBuffer) {

        //AlgoHelper.scaleToOne(audioBuffer);
        AlgoHelper.applyHighPassFilter(audioBuffer);

        //TODO: Can it happen that audioBuffer is not k*4096, e.g. if reading from audio buffer is too fast

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

            timeStep++;
            double[] values = AlgoHelper.fftMagnitude(buffer, win, windowAmp);
            double[] valueForTimeStep = meanExtraction(values, carrierIdx, halfCarrierWidth);

            processFeatureValue(getCurrentHighFeature(), valueForTimeStep[0], true);
            processFeatureValue(getCurrentLowFeature(), valueForTimeStep[1], false);

        }



    }

    private void processFeatureValue(UnrefinedFeature uF, double valueForTimeStep, boolean isHighDoppler) {
        if(valueForTimeStep >= featHighThreshold){
            if(!uF.hasStarted()){
                //start a new feature
                uF.setHasStarted(true);
                uF.setStartTime(timeStep);
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
                            uF.setEndTime(timeStep - 1);
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

        return means;

    }
}
