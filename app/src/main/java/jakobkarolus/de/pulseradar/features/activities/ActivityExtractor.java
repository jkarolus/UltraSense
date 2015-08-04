package jakobkarolus.de.pulseradar.features.activities;

import java.util.List;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Interface for each ActivityExtractor. Given a list of Feature each subclass should decide whether to
 * extract an activity or not and modify the list accordingly.
 *
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public abstract class ActivityExtractor {


    private InferredContextCallback callback;

    public ActivityExtractor(InferredContextCallback callback) {
        this.callback = callback;
    }

    public InferredContextCallback getCallback() {
        return callback;
    }

    /**
     * Detects activities based on the current features.
     * Each subclass should modify the list of features upon detecting an activity
     *
     * @param features the current List of detected features
     * @return Activity (an inferred context) that matches the features
     */
    public abstract void processFeatureList(List<Feature> features);

    /**
     *
     * @return the current InferredContext based on previously seen features
     */
    public abstract InferredContext getCurrentContext();
}
