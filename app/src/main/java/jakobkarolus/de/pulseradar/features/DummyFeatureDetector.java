package jakobkarolus.de.pulseradar.features;

/**
 * <br><br>
 * Created by Jakob on 24.07.2015.
 */
public class DummyFeatureDetector extends FeatureDetector{

    public DummyFeatureDetector(double timeIncreasePerStep) {
        super(timeIncreasePerStep);
    }

    @Override
    public void checkForFeatures(double[] audioBuffer) {
        //do nothing

    }
}
