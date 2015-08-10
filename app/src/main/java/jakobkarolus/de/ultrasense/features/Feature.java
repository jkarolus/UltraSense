package jakobkarolus.de.ultrasense.features;

/**
 * Each Feature defines these three aspects that are used to learn and apply classification.<br>
 * The internal structure (Gaussian curve, ...) is subclass-specific
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public interface Feature {

    /**
     * Length is defined as temporal extends of the feature.<br>
     * E.g. for a Gaussian this equals the std deviation
     *
     * @return the length of this feature
     */
    public double getLength();

    /**
     * Time defines the timestep since detection start when this feature occurred (halfway through point).<br>
     * E.g. for a Gaussian this equals the mean
     *
     * @return the time of this feature
     */
    public double getTime();

    /**
     * Weight is defined as amplitude/impact of the feature. This usually increase with velocity.<br>
     * E.g. for a Gaussian this equals the weight
     *
     * @return the weight of this feature
     */
    public double getWeight();
}
