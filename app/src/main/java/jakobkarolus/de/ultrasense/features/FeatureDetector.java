package jakobkarolus.de.ultrasense.features;

import java.util.List;
import java.util.Vector;

/**
 * Each FeatureDetector processes the current audio stream and notifies its listener when it detects a Feature.<br>
 * Detection is separated for high and low doppler shifts as is their notification.
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureDetector {

    /**
     * keep track of the time
     */
    private double time;
    private double timeIncreasePerStep;


    private List<FeatureExtractor> featExtractors;
    private FeatureProcessor featureProcessor;

    /**
     * the cumulating current features for each high and low doppler
     */
    private UnrefinedFeature currentHighFeature;
    private UnrefinedFeature currentLowFeature;


    /**
     * creates a new FeatureDetector
     * @param timeIncreasePerStep the amount of real time that passes during one time-step (depends on the fft parameters)
     */
    public FeatureDetector(double timeIncreasePerStep, FeatureProcessor featureProcessor){
        this.featExtractors = new Vector<>();
        this.timeIncreasePerStep = timeIncreasePerStep;
        this.currentHighFeature = new UnrefinedFeature(this.timeIncreasePerStep);
        this.currentLowFeature = new UnrefinedFeature(this.timeIncreasePerStep);
        this.featureProcessor = featureProcessor;
    }


    /**
     * process the audioBuffer and detect features. Call notify... upon detection.<br>
     *
     * @param audioBuffer audio data as double[]
     * @param applyHighPass suggestion, whether to use a high pass filter on this data (e.g. preprocessed data for testing that has already been filtered)
     */
    public abstract void checkForFeatures(double[] audioBuffer, boolean applyHighPass);


    /**
     * notifies all Feature Extractors that a positive doppler has been detected
     */
    public void notifyFeatureDetectedHigh(){
        for(FeatureExtractor fe : this.featExtractors) {
            Feature f = fe.onHighFeatureDetected(new UnrefinedFeature(currentHighFeature));
            if(f != null)
                featureProcessor.processFeature(f);
        }

        this.currentHighFeature = new UnrefinedFeature(this.timeIncreasePerStep);
    }

    /**
     * notifies all Feature Extractors that a negative doppler has been detected
     */
    public void notifyFeatureDetectedLow(){
        for(FeatureExtractor fe : this.featExtractors) {
            Feature f = fe.onLowFeatureDetected(new UnrefinedFeature(currentLowFeature));
            if(f != null)
                featureProcessor.processFeature(f);
        }

        this.currentLowFeature = new UnrefinedFeature(this.timeIncreasePerStep);
    }

        public void registerFeatureExtractor(FeatureExtractor featureExtractor){
        this.featExtractors.add(featureExtractor);
    }

    public void unregisterFeatureExtractor(FeatureExtractor featureExtractor){
        this.featExtractors.remove(featureExtractor);
    }

    public void unregisterAll(){
        this.featExtractors.clear();
    }

    protected UnrefinedFeature getCurrentHighFeature() {
        return currentHighFeature;
    }

    protected UnrefinedFeature getCurrentLowFeature() {
        return currentLowFeature;
    }

    protected double getTime() {
        return time;
    }

    protected void setTime(double time) {
        this.time = time;
    }

    protected double getTimeIncreasePerStep() {
        return timeIncreasePerStep;
    }

    protected void setTimeIncreasePerStep(double timeIncreasePerStep) {
        this.timeIncreasePerStep = timeIncreasePerStep;
    }

    protected void increaseTime(){
        this.time += timeIncreasePerStep;
    }

    /**
     *
     * @return a String representation of the internal parameters used for feature detection
     */
    public abstract String printParameters();

}
