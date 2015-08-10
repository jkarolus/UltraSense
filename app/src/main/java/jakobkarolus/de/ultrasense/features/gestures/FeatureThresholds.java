package jakobkarolus.de.ultrasense.features.gestures;

/**
 * data class holding thresholds for a single feature
 * <br><br>
 * Created by Jakob on 29.07.2015.
 */
public class FeatureThresholds {

    private double featureLengthMinThr = Double.MAX_VALUE;
    private double featureLengthMaxThr = -Double.MAX_VALUE;
    private double featureWeightMinThr = Double.MAX_VALUE;
    private double featureWeightMaxThr = -Double.MAX_VALUE;

    protected final static String LENGTH_MIN = "LengthMin";
    protected final static String LENGTH_MAX = "LengthMax";
    protected final static String WEIGHT_MIN = "WeightMin";
    protected final static String WEIGHT_MAX = "WeightMax";

    public FeatureThresholds(double featureLengthMinThr, double featureLengthMaxThr, double featureWeightMinThr, double featureWeightMaxThr) {
        this.featureLengthMinThr = featureLengthMinThr;
        this.featureLengthMaxThr = featureLengthMaxThr;
        this.featureWeightMinThr = featureWeightMinThr;
        this.featureWeightMaxThr = featureWeightMaxThr;
    }

    public FeatureThresholds(){
        this.featureLengthMinThr = Double.MAX_VALUE;
        this.featureLengthMaxThr = -Double.MAX_VALUE;
        this.featureWeightMinThr = Double.MAX_VALUE;
        this.featureWeightMaxThr = -Double.MAX_VALUE;
    }

    public double getFeatureLengthMinThr() {
        return featureLengthMinThr;
    }

    public void setFeatureLengthMinThr(double featureLengthMinThr) {
        this.featureLengthMinThr = featureLengthMinThr;
    }

    public double getFeatureLengthMaxThr() {
        return featureLengthMaxThr;
    }

    public void setFeatureLengthMaxThr(double featureLengthMaxThr) {
        this.featureLengthMaxThr = featureLengthMaxThr;
    }

    public double getFeatureWeightMinThr() {
        return featureWeightMinThr;
    }

    public void setFeatureWeightMinThr(double featureWeightMinThr) {
        this.featureWeightMinThr = featureWeightMinThr;
    }

    public double getFeatureWeightMaxThr() {
        return featureWeightMaxThr;
    }

    public void setFeatureWeightMaxThr(double featureWeightMaxThr) {
        this.featureWeightMaxThr = featureWeightMaxThr;
    }
}
