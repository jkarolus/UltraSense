package jakobkarolus.de.pulseradar.features;

import java.util.List;
import java.util.Vector;

/**
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureDetector {

    private List<FeatureExtractor> featExtractors;
    private UnrefinedFeature currentHighFeature;
    private UnrefinedFeature currentLowFeature;


    public FeatureDetector(){
        this.featExtractors = new Vector<>();
        this.currentHighFeature = new UnrefinedFeature();
        this.currentLowFeature = new UnrefinedFeature();
    }


    public abstract void checkForFeatures(double[] audioBuffer);

    public void notifyFeatureDetectedHigh(){
        for(FeatureExtractor fe : this.featExtractors)
            fe.onHighFeatureDetected(new UnrefinedFeature(currentHighFeature));

        this.currentHighFeature = new UnrefinedFeature();
    }

    public void notifyFeatureDetectedLow(){
        for(FeatureExtractor fe : this.featExtractors)
            fe.onLowFeatureDetected(new UnrefinedFeature(currentLowFeature));

        this.currentLowFeature = new UnrefinedFeature();
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

    public UnrefinedFeature getCurrentHighFeature() {
        return currentHighFeature;
    }

    public UnrefinedFeature getCurrentLowFeature() {
        return currentLowFeature;
    }
}
