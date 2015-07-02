package jakobkarolus.de.pulseradar.features;

import java.util.List;
import java.util.Vector;

/**
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureDetector {

    private List<FeatureExtractor> featExtractors;
    private UnrefinedFeature currentFeature;


    public FeatureDetector(){
        this.featExtractors = new Vector<>();
        this.currentFeature = new UnrefinedFeature();
    }


    public abstract void checkForFeatures(double[] audioBuffer);

    public void notifyFeatureDetected(){
        for(FeatureExtractor fe : this.featExtractors)
            fe.onFeatureDetected(new UnrefinedFeature(currentFeature.getUnrefinedFeature()));

        this.currentFeature = new UnrefinedFeature();
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

    public UnrefinedFeature getCurrentFeature() {
        return currentFeature;
    }

    public void setCurrentFeature(UnrefinedFeature currentFeature) {
        this.currentFeature = currentFeature;
    }
}
