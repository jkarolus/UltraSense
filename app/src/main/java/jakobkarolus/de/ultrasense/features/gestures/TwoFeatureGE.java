package jakobkarolus.de.ultrasense.features.gestures;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * abstracts over gesture that consist of two consecutive doppler features (e.g. swipes).<br>
 * These gesture can be classified using their extend/length and weight of the respective doppler feature
 * and the distance between these.
 *
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public abstract class TwoFeatureGE implements GestureExtractor{

    private final static String TIME_DISTANCE_MIN = "TimeDistanceMin";
    private final static String TIME_DISTANCE_MAX = "TimeDistanceMax";


    private FeatureThresholds featureOneThresholds;
    private FeatureThresholds featureTwoThresholds;

    private double featureTimeDistanceMinThr = Double.MAX_VALUE;
    private double featureTimeDistanceMaxThr = -Double.MAX_VALUE;

    private DecimalFormat df = new DecimalFormat("####0.0000");


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

        boolean sanityCheck = doSanityCalibrationCheck(features);
        if(!sanityCheck) {
            features.clear();
            return CalibrationState.FAILED;
        }

        if(features.size()==1){
            //it's the first feature, "ignore" to keep on stack and wait for the second
            return CalibrationState.ONGOING;
        }

        if(features.size() == 2){
            //sanity is already checked, use the two feature to updat thresholds
            Feature f1 = features.get(0);
            Feature f2 = features.get(1);
            updateFeatureThresholds(featureOneThresholds, f1);
            updateFeatureThresholds(featureTwoThresholds, f2);
            updateTimeDistance(f1.getTime(), f2.getTime());
            features.clear();
            return CalibrationState.SUCCESSFUL;

        }

        //cant fit a two motion gesture
        return CalibrationState.FAILED;
    }

    private void updateTimeDistance(double featureOneTime, double featureTwoTime) {

        double dist = featureTwoTime - featureOneTime;
        if(dist <= featureTimeDistanceMinThr)
            featureTimeDistanceMinThr = dist;
        if(dist >= featureTimeDistanceMaxThr)
            featureTimeDistanceMaxThr = dist;

    }

    private void updateFeatureThresholds(FeatureThresholds ft, Feature f){

        if(f.getLength() <= ft.getFeatureLengthMinThr())
            ft.setFeatureLengthMinThr(f.getLength());

        if(f.getLength() >= ft.getFeatureLengthMaxThr())
            ft.setFeatureLengthMaxThr(f.getLength());

        if(f.getWeight() <= ft.getFeatureWeightMinThr())
            ft.setFeatureWeightMinThr(f.getWeight());

        if(f.getWeight() >= ft.getFeatureWeightMaxThr())
            ft.setFeatureWeightMaxThr(f.getWeight());

    }

    @Override
    public void finishCalibration() {
        //slightly increase the thresholds to cope with variations
        adjustFeatureThresholdsAfterCalibration(featureOneThresholds);
        adjustFeatureThresholdsAfterCalibration(featureTwoThresholds);
        featureTimeDistanceMinThr = Math.max(0.01, featureTimeDistanceMinThr - 0.05);
        featureTimeDistanceMaxThr = featureTimeDistanceMaxThr + 0.05;


    }

    private void adjustFeatureThresholdsAfterCalibration(FeatureThresholds ft){
        ft.setFeatureLengthMinThr(Math.max(0.01, ft.getFeatureLengthMinThr() - 0.02));
        ft.setFeatureLengthMaxThr(ft.getFeatureLengthMaxThr() + 0.02);
        if(ft.getFeatureWeightMaxThr() > 0.0){
            ft.setFeatureWeightMinThr(Math.max(0.1, ft.getFeatureWeightMinThr() - 0.3));
            ft.setFeatureWeightMaxThr(ft.getFeatureWeightMaxThr() + 0.3);
        }
        else{
            ft.setFeatureWeightMaxThr(Math.min(-0.1, ft.getFeatureWeightMaxThr() + 0.3));
            ft.setFeatureWeightMinThr(ft.getFeatureWeightMinThr() - 0.3);
        }
    }

    @Override
    public String getThresholds() {

        StringBuffer buffer = new StringBuffer();
        buffer.append("FirstLength: " + df.format(featureOneThresholds.getFeatureLengthMinThr()) + " <-> " + df.format(featureOneThresholds.getFeatureLengthMaxThr()));
        buffer.append("; FirstWeight: " + df.format(featureOneThresholds.getFeatureWeightMinThr()) + " <-> " + df.format(featureOneThresholds.getFeatureWeightMaxThr()));
        buffer.append("; SecondLength: " + df.format(featureTwoThresholds.getFeatureLengthMinThr()) + " <-> " + df.format(featureTwoThresholds.getFeatureLengthMaxThr()));
        buffer.append("; SecondWeight: " + df.format(featureTwoThresholds.getFeatureWeightMinThr()) + " <-> " + df.format(featureTwoThresholds.getFeatureWeightMaxThr()));
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
        map.put(identifier + FeatureThresholds.LENGTH_MIN, ft.getFeatureLengthMinThr());
        map.put(identifier + FeatureThresholds.LENGTH_MAX, ft.getFeatureLengthMaxThr());
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
