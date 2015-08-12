package jakobkarolus.de.ultrasense.features;

/**
 * Each Feature defines these three aspects that are used to learn and apply classification.<br>
 * The internal structure (Gaussian curve, ...) is subclass-specific.<br>
 * Additionally an extractorId is used to discern features from different FeatureExtractors
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public abstract class Feature {

    private int extractorId;

    public Feature(int extractorId) {
        this.extractorId = extractorId;
    }

    /**
     * Length is defined as temporal extends of the feature.<br>
     * E.g. for a Gaussian this equals the std deviation
     *
     * @return the length of this feature
     */
    public abstract double getLength();

    /**
     * Time defines the timestep since detection start when this feature occurred (halfway through point).<br>
     * E.g. for a Gaussian this equals the mean
     *
     * @return the time of this feature
     */
    public abstract double getTime();

    /**
     * Weight is defined as amplitude/impact of the feature. This usually increase with velocity.<br>
     * E.g. for a Gaussian this equals the weight
     *
     * @return the weight of this feature
     */
    public abstract double getWeight();

    /**
     * the id is used to discern the features of different FeatureExtractors during feature processing
     *
     * @return the identifier of the FeatureExtractor that created this feature
     */
    public int getExtractorId(){
        return extractorId;
    }
}
