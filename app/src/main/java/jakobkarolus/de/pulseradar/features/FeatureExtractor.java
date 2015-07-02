package jakobkarolus.de.pulseradar.features;

/**
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureExtractor {

    private FeatureProcessor featProc;

    public FeatureExtractor(FeatureProcessor featProc) {
        this.featProc = featProc;
    }

    public abstract void onFeatureDetected(UnrefinedFeature uF);

    public FeatureProcessor getFeatProc() {
        return featProc;
    }

    public void setFeatProc(FeatureProcessor featProc) {
        this.featProc = featProc;
    }
}
