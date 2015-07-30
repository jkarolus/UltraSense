package jakobkarolus.de.pulseradar.features.gestures;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Created by Jakob on 08.07.2015.
 */
public abstract class TwoMotionGE implements GestureExtractor{

    private final static String TIME_DISTANCE_MIN = "TimeDistanceMin";
    private final static String TIME_DISTANCE_MAX = "TimeDistanceMax";


    private FeatureThresholds featureOneThresholds;
    private FeatureThresholds featureTwoThresholds;

    private double featureTimeDistanceMinThr = Double.MAX_VALUE;
    private double featureTimeDistanceMaxThr = -Double.MAX_VALUE;

    private DecimalFormat df = new DecimalFormat("0.0000E0");


    @Override
    public List<Gesture> detectGesture(List<Feature> features) {
        List<Gesture> gestures = new Vector<>();
        ListIterator<Feature> iter = features.listIterator();

        while(iter.hasNext()) {
            Feature f = iter.next();

            if(applyFeatureThresholds(f, featureOneThresholds)){
                if(iter.hasNext()){
                    Feature next = iter.next();
                    if(applyFeatureThresholds(next, featureTwoThresholds)){
                        if(checkDistanceBetweenFeatures(next, f)){
                            gestures.add(getSpecificGesture());
                            iter.remove();
                            iter.previous();
                            iter.remove();
                        }
                    }
                }
            }
        }

        return gestures;
    }

    private boolean checkDistanceBetweenFeatures(Feature f1, Feature f2) {
        double distance = f1.getTime() - f2.getTime();
        if(distance >= featureTimeDistanceMinThr && distance <= featureTimeDistanceMaxThr)
            return true;
        return false;
    }

    private boolean applyFeatureThresholds(Feature f, FeatureThresholds ft) {
        if(f.getLength() >= ft.getFeatureLengthMinThr() && f.getLength() <= ft.getFeatureLengthMaxThr()) {
            if (f.getWeight() >= ft.getFeatureWeightMinThr() && f.getWeight() <= ft.getFeatureWeightMaxThr()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CalibrationState calibrate(List<Feature> features) {
        //TODO:implement
        return CalibrationState.SUCCESSFUL;
    }

    @Override
    public String getThresholds() {

        StringBuffer buffer = new StringBuffer();
        buffer.append("FirstLength: " + df.format(featureOneThresholds.getFeatureLengthMinThr()) + " <-> " + df.format(featureOneThresholds.getFeatureLengthMaxThr()));
        buffer.append("; FirstWeight: " + df.format(featureOneThresholds.getFeatureWeightMinThr()) + " <-> " + df.format(featureOneThresholds.getFeatureWeightMaxThr()));
        buffer.append("; SecondLength: " + df.format(featureTwoThresholds.getFeatureLengthMinThr()) + " <-> " + df.format(featureTwoThresholds.getFeatureLengthMaxThr()));
        buffer.append("; SecondtWeight: " + df.format(featureTwoThresholds.getFeatureWeightMinThr()) + " <-> " + df.format(featureTwoThresholds.getFeatureWeightMaxThr()));
        buffer.append("; Time Dist.: " + df.format(featureTimeDistanceMinThr) + " <-> " + df.format(featureTimeDistanceMaxThr) + "\n");
        return buffer.toString();
    }

    @Override
    public Map<String, Double> getThresholdMap() {
        Map<String, Double> map = new HashMap<>();
        getThresholdsForMap("First", featureOneThresholds, map);
        getThresholdsForMap("Second", featureTwoThresholds, map);
        map.put(TIME_DISTANCE_MIN, featureTimeDistanceMinThr);
        map.put(TIME_DISTANCE_MAX, featureTimeDistanceMaxThr);
        return map;
    }

    private void getThresholdsForMap(String identifier, FeatureThresholds ft, Map<String, Double> map) {
        map.put(identifier+FeatureThresholds.LENGTH_MIN, ft.getFeatureLengthMinThr());
        map.put(identifier+FeatureThresholds.LENGTH_MAX, ft.getFeatureLengthMaxThr());
        map.put(identifier+FeatureThresholds.WEIGHT_MIN, ft.getFeatureWeightMinThr());
        map.put(identifier+FeatureThresholds.WEIGHT_MAX, ft.getFeatureWeightMaxThr());

    }

    @Override
    public boolean setThresholds(Map<String, Double> thresholds) {

        if(thresholds.containsKey("First" + FeatureThresholds.LENGTH_MIN) && thresholds.containsKey("First" + FeatureThresholds.LENGTH_MAX) && thresholds.containsKey("First" + FeatureThresholds.WEIGHT_MIN) && thresholds.containsKey("First" + FeatureThresholds.WEIGHT_MAX)
                && thresholds.containsKey("Second" + FeatureThresholds.LENGTH_MIN) && thresholds.containsKey("Second" + FeatureThresholds.LENGTH_MAX) && thresholds.containsKey("Second" + FeatureThresholds.WEIGHT_MIN) && thresholds.containsKey("Second" + FeatureThresholds.WEIGHT_MAX)
                && thresholds.containsKey(TIME_DISTANCE_MIN) && thresholds.containsKey(TIME_DISTANCE_MAX)){
            featureOneThresholds = new FeatureThresholds(thresholds.get("First" + FeatureThresholds.LENGTH_MIN), thresholds.get("First" + FeatureThresholds.LENGTH_MAX), thresholds.get("First" + FeatureThresholds.WEIGHT_MIN), thresholds.get("First" + FeatureThresholds.WEIGHT_MAX));
            featureTwoThresholds = new FeatureThresholds(thresholds.get("Second" + FeatureThresholds.LENGTH_MIN), thresholds.get("Second" + FeatureThresholds.LENGTH_MAX), thresholds.get("Second" + FeatureThresholds.WEIGHT_MIN), thresholds.get("Second" + FeatureThresholds.WEIGHT_MAX));
            featureTimeDistanceMinThr = thresholds.get(TIME_DISTANCE_MIN);
            featureTimeDistanceMaxThr = thresholds.get(TIME_DISTANCE_MAX);
            return true;
        }
        else
            return false;
    }


    @Override
    public void resetThresholds() {
        featureOneThresholds = new FeatureThresholds();
        featureTwoThresholds = new FeatureThresholds();
        featureTimeDistanceMinThr = Double.MAX_VALUE;
        featureTimeDistanceMaxThr = -Double.MAX_VALUE;

    }

    protected void setFeatureOneThresholds(FeatureThresholds featureOneThresholds) {
        this.featureOneThresholds = featureOneThresholds;
    }

    protected void setFeatureTwoThresholds(FeatureThresholds featureTwoThresholds) {
        this.featureTwoThresholds = featureTwoThresholds;
    }

    protected void setFeatureTimeDistanceMinThr(double featureTimeDistanceMinThr) {
        this.featureTimeDistanceMinThr = featureTimeDistanceMinThr;
    }

    protected void setFeatureTimeDistanceMaxThr(double featureTimeDistanceMaxThr) {
        this.featureTimeDistanceMaxThr = featureTimeDistanceMaxThr;
    }
}
