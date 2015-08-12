package jakobkarolus.de.ultrasense.features.gestures;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * GestureExtractor recognizing upwards Gestures
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public class UpGE extends OneFeatureGE {

    private static final double LENGTH_MIN = 0.1;
    private static final double LENGTH_MAX = 0.21;
    private static final double WEIGHT_MIN = -5.1;
    private static final double WEIGHT_MAX = -2.1;


    public UpGE(){
        setFeatureLengthMinThr(LENGTH_MIN);
        setFeatureLengthMaxThr(LENGTH_MAX);
        setFeatureWeightMinThr(WEIGHT_MIN);
        setFeatureWeightMaxThr(WEIGHT_MAX);

    }


    @Override
    public Gesture getSpecificGesture() {
        return Gesture.UP;
    }

    @Override
    public boolean doSanityCalibrationCheck(List<Feature> features) {
        if(features.size() != 1)
            return false;

        Feature f = features.get(0);
        if(f.getWeight() >= 0.0)
            return false;

        return true;
    }

    @Override
    public String getName() {
        return UpGE.class.getSimpleName();
    }
}
