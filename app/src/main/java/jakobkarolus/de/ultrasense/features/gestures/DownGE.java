package jakobkarolus.de.ultrasense.features.gestures;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * GestureExtractor recognizing downwards Gestures
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public class DownGE extends OneFeatureGE {


    private static final double LENGTH_MIN = 0.08;
    private static final double LENGTH_MAX = 0.18;
    private static final double WEIGHT_MIN = 2.1;
    private static final double WEIGHT_MAX = 5.1;


    public DownGE(){
        setFeatureLengthMinThr(LENGTH_MIN);
        setFeatureLengthMaxThr(LENGTH_MAX);
        setFeatureWeightMinThr(WEIGHT_MIN);
        setFeatureWeightMaxThr(WEIGHT_MAX);

    }

    @Override
    public Gesture getSpecificGesture() {
        return Gesture.DOWN;
    }

    @Override
    public boolean doSanityCalibrationCheck(List<Feature> features) {

        if (features.size() != 1)
            return false;

        Feature f = features.get(0);
        if (f.getWeight() <= 0.0)
            return false;

        return true;
    }

    @Override
    public String getName() {
        return DownGE.class.getSimpleName();
    }
}
