package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;

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
}
