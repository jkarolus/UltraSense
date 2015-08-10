package jakobkarolus.de.ultrasense.features;

/**
 *
 * Implements a listener to the observable FeatureDetector.<br>
 * Each subclass should create its own Feature representation upon notify().
 *<br>
 *Holds a reference to FeatureProcessor to process individual Feature
 *
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureExtractor {

    private FeatureProcessor featProcessor;

    public FeatureExtractor(FeatureProcessor featProcessor) {
        this.featProcessor = featProcessor;
    }

    public abstract void onHighFeatureDetected(UnrefinedFeature uF);

    public abstract void onLowFeatureDetected(UnrefinedFeature uF);


    public FeatureProcessor getFeatProcessor() {
        return featProcessor;
    }

    public void setFeatProcessor(FeatureProcessor featProcessor) {
        this.featProcessor = featProcessor;
    }
}
