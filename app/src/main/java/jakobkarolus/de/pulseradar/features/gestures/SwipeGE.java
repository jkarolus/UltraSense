package jakobkarolus.de.pulseradar.features.gestures;

/**
 * handles swipes gestures above the phone
 * <br><br>
 * Created by Jakob on 29.07.2015.
 */
public class SwipeGE extends TwoMotionGE{

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
}
