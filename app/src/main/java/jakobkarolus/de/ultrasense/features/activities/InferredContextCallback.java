package jakobkarolus.de.ultrasense.features.activities;

/**
 * Callback interface for ActivityExtractors upon detecting a context change
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public interface InferredContextCallback {

    /**
     * ActivityExtractors should call this method upon context/activity changes
     * @param oldContext the old inferred context
     * @param newContext the new inferred context
     * @param reason possibility to display a reason to the user
     */
    public void onInferredContextChange(InferredContext oldContext, InferredContext newContext, String reason);
}
