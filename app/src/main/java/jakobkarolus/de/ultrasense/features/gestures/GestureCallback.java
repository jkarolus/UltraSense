package jakobkarolus.de.ultrasense.features.gestures;

import java.util.Map;

/**
 * abstract hook methods getting called when a gesture is recognized or to inform the user during calibration
 * <br><br>
 * Created by Jakob on 30.07.2015.
 */
public interface GestureCallback {

    /**
     * callback during a multiple-step calibration
     * @param calibState the state of the calibration (e.g. ongoing when calibrating two-feature gestures (swipe))
     */
    public void onCalibrationStep(final CalibrationState calibState);


    /**
     * callback upon successful completion of a full calibration
     * @param thresholds a map of thresholds name to value (can be displayed, saved, etc.)
     * @param prettyPrintThresholds pretty print of the above map
     * @param name the name of the calibrated GestureExtractor
     */
    public void onCalibrationFinished(final Map<String, Double> thresholds, final String prettyPrintThresholds, final String name);


    /**
     * callback when one of the GestureExtractor detects a gesture
     * @param gesture the Gesture that was detected
     */
    public void onGestureDetected(final Gesture gesture);

}
