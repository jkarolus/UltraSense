package jakobkarolus.de.ultrasense.features.gestures;

/**
 * possible state that be taken during calibration.<br>
 * For OneFeatureGE the only valid ones are SUCCESSFUL and FAILED (cause one single Feature describes the gesture).<br>
 * For TwoFeatureGE the state ONGOING is possible when waiting for another feature
 * <br><br>
 * Created by Jakob on 30.07.2015.
 */
public enum CalibrationState {

    ONGOING,
    SUCCESSFUL,
    FAILED;
}
