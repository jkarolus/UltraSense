package jakobkarolus.de.pulseradar.features.gestures;

/**
 * Wave-like gesture performing first a Down then an Up gesture
 * <br><br>
 * Created by Jakob on 29.07.2015.
 */
public class DownUpGE extends TwoMotionGE{


    /**
     * n7:
     *
     *     private static final double F1_LENGTH_MIN = 0.06;//0.08;
     private static final double F1_LENGTH_MAX = 0.12;//0.18;
     private static final double F1_WEIGHT_MIN = 0.5;//2.8;
     private static final double F1_WEIGHT_MAX = 2.0;//4.7;

     private static final double F2_LENGTH_MIN = 0.04;//0.08;
     private static final double F2_LENGTH_MAX = 0.13;//1.9;
     private static final double F2_WEIGHT_MIN = -1.7;//-4.8;
     private static final double F2_WEIGHT_MAX = -0.4;//-3.1;

     private static final double TIME_DIST_MIN = 0.3;//0.34;
     private static final double TIME_DIST_MAX = 0.9;//0.63;
     */

    private static final double F1_LENGTH_MIN = 0.08;
    private static final double F1_LENGTH_MAX = 0.18;
    private static final double F1_WEIGHT_MIN = 2.8;
    private static final double F1_WEIGHT_MAX = 4.7;

    private static final double F2_LENGTH_MIN = 0.08;
    private static final double F2_LENGTH_MAX = 1.9;
    private static final double F2_WEIGHT_MIN = -4.8;
    private static final double F2_WEIGHT_MAX = -3.1;

    private static final double TIME_DIST_MIN = 0.34;
    private static final double TIME_DIST_MAX = 0.63;


    public DownUpGE(){

        setFeatureOneThresholds(new FeatureThresholds(F1_LENGTH_MIN, F1_LENGTH_MAX, F1_WEIGHT_MIN, F1_WEIGHT_MAX));
        setFeatureTwoThresholds(new FeatureThresholds(F2_LENGTH_MIN, F2_LENGTH_MAX, F2_WEIGHT_MIN, F2_WEIGHT_MAX));
        setFeatureTimeDistanceMinThr(TIME_DIST_MIN);
        setFeatureTimeDistanceMaxThr(TIME_DIST_MAX);
    }


    @Override
    public String getName() {
        return DownUpGE.class.getSimpleName();
    }

    @Override
    public Gesture getSpecificGesture() {
        return Gesture.DOWN_UP;
    }
}
