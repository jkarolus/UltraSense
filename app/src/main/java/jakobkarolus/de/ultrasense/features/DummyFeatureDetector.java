package jakobkarolus.de.ultrasense.features;

/**
 * FeatureDetector that does detect any features; Is used during FMCW mode
 * <br><br>
 * Created by Jakob on 24.07.2015.
 */
public class DummyFeatureDetector extends FeatureDetector{


    /**
     * creates a new DummyFeatureDetector
     *
     * @param timeIncreasePerStep the amount of real time that passes during one time-step (depends on the fft parameters)
     */
    public DummyFeatureDetector(double timeIncreasePerStep) {
        super(timeIncreasePerStep, new DummyFeatureProcessor());
    }

    @Override
    public void checkForFeatures(double[] audioBuffer, boolean applyHighPass) {
        //do nothing

    }

    @Override
    public String printParameters() {
        return "FD paras: Dummy";
    }
}
