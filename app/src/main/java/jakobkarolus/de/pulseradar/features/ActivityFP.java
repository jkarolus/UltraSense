package jakobkarolus.de.pulseradar.features;

import java.util.List;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.activities.ActivityExtractor;

/**
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public class ActivityFP extends FeatureProcessor{

    private static double TIME_THRESHOLD = 10.0;

    private List<ActivityExtractor> activityExtractors;

    public ActivityFP() {
        this.activityExtractors = new Vector<>();
    }

    public void registerActivityExtractor(ActivityExtractor ae){
        this.activityExtractors.add(ae);
    }

    public void unregisterActivityExtractor(ActivityExtractor ae){
        this.activityExtractors.remove(ae);
    }

    public void unregisterAllActivityExtractors(){
        this.activityExtractors.clear();
    }

    @Override
    public void processFeatureOnSubclass(Feature feature) {

        for(ActivityExtractor ae : activityExtractors){
            ae.processFeatureList(getFeatures());
        }
        cleanUpFeatureStack();

    }

    @Override
    public double getTimeThresholdForGC() {
        return TIME_THRESHOLD;
    }
}
