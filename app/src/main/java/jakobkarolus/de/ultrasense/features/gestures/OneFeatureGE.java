package jakobkarolus.de.ultrasense.features.gestures;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * abstracts over gesture that only consist of one simple motion (like going down or up).<br>
 * These gesture can be classified using their extend/length and weight
 *
 * <br><br>
 * Created by Jakob on 28.07.2015.
 */
public abstract class OneFeatureGE implements GestureExtractor{

    private final static String LENGTH_MIN = "LengthMin";
    private final static String LENGTH_MAX = "LengthMax";
    private final static String WEIGHT_MIN = "WeightMin";
    private final static String WEIGHT_MAX = "WeightMax";


    private double featureLengthMinThr = Double.MAX_VALUE;
    private double featureLengthMaxThr = -Double.MAX_VALUE;
    private double featureWeightMinThr = Double.MAX_VALUE;
    private double featureWeightMaxThr = -Double.MAX_VALUE;


    private DecimalFormat df = new DecimalFormat("####0.0000");

    @Override
    public boolean setThresholds(Map<String, Double> thresholds) {

        if(thresholds.containsKey(LENGTH_MIN) && thresholds.containsKey(LENGTH_MAX) && thresholds.containsKey(WEIGHT_MIN) && thresholds.containsKey(WEIGHT_MAX)){
            featureLengthMinThr = thresholds.get(LENGTH_MIN);
            featureLengthMaxThr = thresholds.get(LENGTH_MAX);
            featureWeightMinThr = thresholds.get(WEIGHT_MIN);
            featureWeightMaxThr = thresholds.get(WEIGHT_MAX);
            return true;
        }
        else
            return false;
    }

    @Override
    public List<Gesture> detectGesture(List<Feature> features) {
        List<Gesture> gestures = new Vector<>();
        ListIterator<Feature> iter = features.listIterator();
        while(iter.hasNext()){
            Feature f = iter.next();
            if(f.getLength() >= featureLengthMinThr && f.getLength() <= featureLengthMaxThr) {
                if (f.getWeight() >= featureWeightMinThr  && f.getWeight() <= featureWeightMaxThr) {
                    gestures.add(getSpecificGesture());
                    iter.remove();
                }
            }
        }
        return gestures;
    }


    @Override
    public CalibrationState calibrate(List<Feature> features) {

        boolean sanityCheck = doSanityCalibrationCheck(features);
        if(!sanityCheck) {
            features.clear();
            return CalibrationState.FAILED;
        }

        //oneMotion gesture only have one feature
        Feature f = features.get(0);
        if(f.getWeight() <= featureWeightMinThr)
            featureWeightMinThr = f.getWeight();

        if(f.getWeight() >= featureWeightMaxThr)
            featureWeightMaxThr = f.getWeight();

        if(f.getLength() <= featureLengthMinThr)
            featureLengthMinThr = f.getLength();

        if(f.getLength() >= featureLengthMaxThr)
            featureLengthMaxThr = f.getLength();

        //consume feature
        features.clear();

        return CalibrationState.SUCCESSFUL;
    }

    @Override
    public void finishCalibration() {
        //slightly increase the thresholds to cope with variations
        featureLengthMinThr = Math.max(0.01, featureLengthMinThr - 0.02);
        featureLengthMaxThr = featureLengthMaxThr + 0.02;
        if(featureWeightMaxThr > 0.0){
            featureWeightMinThr = Math.max(0.1, featureWeightMinThr - 0.3);
            featureWeightMaxThr = featureWeightMaxThr + 0.3;
        }
        else{
            featureWeightMaxThr = Math.min(-0.1, featureWeightMaxThr + 0.3);
            featureWeightMinThr = featureWeightMinThr - 0.3;
        }

    }

    @Override
    public String getThresholds(){
        return "Length: " + df.format(featureLengthMinThr) + " <-> " + df.format(featureLengthMaxThr) + "; Weight: " + df.format(featureWeightMinThr) + " <-> " + df.format(featureWeightMaxThr) + "\n";
    }

    @Override
    public Map<String, Double> getThresholdMap() {
        Map<String, Double> map = new HashMap<>();
        map.put(LENGTH_MIN, featureLengthMinThr);
        map.put(LENGTH_MAX, featureLengthMaxThr);
        map.put(WEIGHT_MIN, featureWeightMinThr);
        map.put(WEIGHT_MAX, featureWeightMaxThr);
        return map;
    }

    @Override
    public void resetThresholds() {
        featureLengthMinThr = Double.MAX_VALUE;
        featureLengthMaxThr = -Double.MAX_VALUE;
        featureWeightMinThr = Double.MAX_VALUE;
        featureWeightMaxThr = -Double.MAX_VALUE;
    }

    protected void setFeatureLengthMinThr(double featureLengthMinThr) {
        this.featureLengthMinThr = featureLengthMinThr;
    }

    protected void setFeatureLengthMaxThr(double featureLengthMaxThr) {
        this.featureLengthMaxThr = featureLengthMaxThr;
    }

    protected void setFeatureWeightMinThr(double featureWeightMinThr) {
        this.featureWeightMinThr = featureWeightMinThr;
    }

    protected void setFeatureWeightMaxThr(double featureWeightMaxThr) {
        this.featureWeightMaxThr = featureWeightMaxThr;
    }
}
