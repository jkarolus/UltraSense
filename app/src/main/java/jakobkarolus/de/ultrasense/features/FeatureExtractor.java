package jakobkarolus.de.ultrasense.features;

/**
 *
 * Implements a listener to the observable FeatureDetector.<br>
 * Each subclass should create its own Feature representation upon getting notified.
 * <br>
 *
 * Created by Jakob on 02.07.2015.
 */
public abstract class FeatureExtractor {

    private int id;


    /**
     * creates a new FeatureExtractor with the given id.<br>
     * The id used to discern different FEs when passing their feature to the FeatureProcessor.
     *
     * @param id identifier for this specific FeatureExtractor
     */
    public FeatureExtractor(int id) {
        this.id = id;
    }

    /**
     * notify method, which gets called when a positive doppler shift was detected
     * @param uF the UnrefinedFeature encapsulating the data
     * @return Feature corresponding to the data, or null if not applicable
     */
    public abstract Feature onHighFeatureDetected(UnrefinedFeature uF);

    /**
     * notify method, which gets called when a negative doppler shift was detected
     * @param uF the UnrefinedFeature encapsulating the data
     * @return Feature corresponding to the data, or null if not applicable
     */
    public abstract Feature onLowFeatureDetected(UnrefinedFeature uF);

    /**
     *
     * @return the identifier for this FeatureExtractor
     */
    public int getId() {
        return id;
    }


}
