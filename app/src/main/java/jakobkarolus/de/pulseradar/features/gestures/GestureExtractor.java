package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;
import java.util.Map;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Interface for each GestureExtrator. Given a list of Feature each subclass should decide whether to
 * extract a feature or not and modify the list accordingly.
 *
 * <br><br>
 * Created by Jakob on 07.07.2015.
 */
public interface GestureExtractor {

    /**
     * Detects Gestures based on the current features.
     * Each subclass should modify the list of features upon detecting a gesture
     *
     * @param features the current List of detected features
     * @return a list of gestures that match the features
     */
    public List<Gesture> detectGesture(List<Feature> features);

    /**
     * use the given list of features to calibrate thresholds.<br>
     * GestureExtractor should do a sanity check on the given list and return
     * whether they used the stack for calibration
     *
     * @param features the current List of detected features
     * @return true if the list corresponds to the gesture; false otherwise
     */
    public boolean calibrate(List<Feature> features);


    /**
     * pretty print of the current used thresholds, for debugging
     *
     * @return String containing all thresholds used by this GestureExtractor
     */
    public String getThresholds();

    /**
     *
     * @return thresholds for this GE as a Map
     */
    public Map<String, Double> getThresholdMap();

    /**
     * sets the internal thresholds for feature detection
     * @param thresholds a map of threshold name to value
     * @return whether the thresholds could be applied
     */
    public boolean setThresholds(Map<String, Double> thresholds);

    /**
     *
     * @return the name of this GestureExtractor
     */
    public String getName();

}
