package jakobkarolus.de.ultrasense.features.gestures;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * handles swipes gestures above the phone
 * <br><br>
 * Created by Jakob on 29.07.2015.
 */
public class SwipeGE extends TwoFeatureGE {

    /*
    private static final double F1_LENGTH_MIN = 0.023;
    private static final double F1_LENGTH_MAX = 0.1;
    private static final double F1_WEIGHT_MIN = 0.2;
    private static final double F1_WEIGHT_MAX = 2.5;

    private static final double F2_LENGTH_MIN = 0.02;
    private static final double F2_LENGTH_MAX = 0.11;
    private static final double F2_WEIGHT_MIN = -2.5;
    private static final double F2_WEIGHT_MAX = -0.2;

    private static final double TIME_DIST_MIN = 0.05;
    private static final double TIME_DIST_MAX = 0.28;
    */

    /**noisy env, allso works good for silent env (they use different FD paras though)
     *
     */
    private static final double F1_LENGTH_MIN = 0.02;
    private static final double F1_LENGTH_MAX = 0.13;
    private static final double F1_WEIGHT_MIN = 0.2;
    private static final double F1_WEIGHT_MAX = 3.0;

    private static final double F2_LENGTH_MIN = 0.02;
    private static final double F2_LENGTH_MAX = 0.13;
    private static final double F2_WEIGHT_MIN = -3.0;
    private static final double F2_WEIGHT_MAX = -0.2;

    private static final double TIME_DIST_MIN = 0.05;
    private static final double TIME_DIST_MAX = 0.28;


    public SwipeGE(){

        setFeatureOneThresholds(new FeatureThresholds(F1_LENGTH_MIN, F1_LENGTH_MAX, F1_WEIGHT_MIN, F1_WEIGHT_MAX));
        setFeatureTwoThresholds(new FeatureThresholds(F2_LENGTH_MIN, F2_LENGTH_MAX, F2_WEIGHT_MIN, F2_WEIGHT_MAX));
        setFeatureTimeDistanceMinThr(TIME_DIST_MIN);
        setFeatureTimeDistanceMaxThr(TIME_DIST_MAX);
    }


    @Override
    public String getName() {
        return SwipeGE.class.getSimpleName();
    }

    @Override
    public Gesture getSpecificGesture() {
        return Gesture.SWIPE;
    }

    @Override
    public boolean doSanityCalibrationCheck(List<Feature> features) {

        if(features.size() == 1) {
            //the first feature has to be a down feature
            Feature f = features.get(0);
            if (f.getWeight() >= 0.0)
                return true;
        }

        if(features.size() == 2){
            Feature f1 = features.get(0);
            Feature f2 = features.get(1);
            if (f1.getWeight() >= 0.0 && f2.getWeight() <= 0.0)
                return true;
        }

        return false;
    }
}
