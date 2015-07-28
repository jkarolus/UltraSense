package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * GestureExtractor recognizing upwards Gestures
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public class UpGE extends OneMotionGE{


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
