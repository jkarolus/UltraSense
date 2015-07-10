package jakobkarolus.de.pulseradar.features.gestures;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Created by Jakob on 08.07.2015.
 */
public class SwipeGE extends DownUpGE{

    private static final double SWIPE_THRESHOLD_HIGH = 4.5;
    private static final double SWIPE_THRESHOLD_LOW = 2.0;


    //TODO: implement for different kind of swipes

    @Override
    protected boolean isDownGesture(Feature f) {
        if(f.getLength() >= 0.4 && f.getLength() <= 1.5) {
            if (f.getWeight() >= 10.0 && f.getWeight() <= 30.0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isUpGesture(Feature f) {
        if(f.getLength() >= 0.4 && f.getLength() <= 1.5) {
            if (f.getWeight() >= -25.0 && f.getWeight() <= -5.0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected double getHighThreshold() {
        return SWIPE_THRESHOLD_HIGH;
    }

    @Override
    protected double getLowThreshold() {
        return SWIPE_THRESHOLD_LOW;
    }

    @Override
    protected Gesture getGesture() {
        return Gesture.SWIPE;
    }
}
