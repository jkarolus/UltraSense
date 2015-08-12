package jakobkarolus.de.ultrasense.features.gestures;

import java.util.List;
import java.util.Map;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * Interface for each GestureExtractor. Given a list of Features each subclass should decide whether to
 * extract a gesture or not and modify the list accordingly.
 *
 * <br><br>
 * Created by Jakob on 07.07.2015.
 */
public interface GestureExtractor {

    /**
     * Detects Gestures based on the current features.
     * Each subclass should modify the list of features upon detecting a gesture.<br>
     * Gets called by the GestureFP every time a new feature arrives
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
     * @return CalibrationState indicating the state of the calibration
     */
    public CalibrationState calibrate(List<Feature> features);


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

    /**
     * called during calibration to reset the previous thresholds
     */
    void resetThresholds();

    /**
     *
     * @return the subclass specific Gesture from the enum
     */
    public Gesture getSpecificGesture();

    /**
     * sanity check for subclass specific gestures during calibration (e.g. high doppler -> down feature)
     * @param features the feature list
     * @return true if feature is possible; otherwise false (e.g. low doppler and down feature)
     */
    public boolean doSanityCalibrationCheck(List<Feature> features);

    /**
     * gets called upon finishing the calibration.<br>
     * GestureExtractor may combine the collected thresholds during calibration into the final ones
     */
    public void finishCalibration();
}
