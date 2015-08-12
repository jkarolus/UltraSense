package jakobkarolus.de.ultrasense.features;

/**
 *
 * Implements a listener to the observable FeatureDetector.<br>
 * Each subclass should create its own Feature representation upon getting notified.
 *<br>
 *Holds a reference to FeatureProcessor to process individual Features
 *
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureExtractor {

    private FeatureProcessor featProcessor;

    public FeatureExtractor(FeatureProcessor featProcessor) {
        this.featProcessor = featProcessor;
    }

    /**
     * notify method, which gets called when a positive doppler shift was detected
     * @param uF the UnrefinedFeature encapsulating the data
     */
    public abstract void onHighFeatureDetected(UnrefinedFeature uF);

    /**
     * notify method, which gets called when a negative doppler shift was detected
     * @param uF the UnrefinedFeature encapsulating the data
     */
    public abstract void onLowFeatureDetected(UnrefinedFeature uF);


    public FeatureProcessor getFeatProcessor() {
        return featProcessor;
    }

}
