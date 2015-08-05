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
     * Gets called by an ActivityFP everytime a new feature is detected.<br>
     *
     * @param feature the newest feature that was detected
     * @return true if the feature has been consumed; otherwise false
     */
    public abstract boolean processNewFeature(Feature feature);

    /**
     * Detects activities based on the current features.<br>
     * Contrary to the GestureFP, this method is called in regular time intervals to enable time sensitive context.<br>
     * ActivityExtractor should modify the list upon detecting an activity!
     *
     * @param features the current feature list
     */
    public abstract void processFeatureList(List<Feature> features);

    /**
     *
     * @return the current InferredContext based on previously seen features
     */
    public abstract InferredContext getCurrentContext();
}
