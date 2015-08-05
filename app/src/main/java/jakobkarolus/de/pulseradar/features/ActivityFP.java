package jakobkarolus.de.pulseradar.features;

import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.activities.ActivityExtractor;

/**
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public class ActivityFP extends FeatureProcessor{

    private static double TIME_THRESHOLD = 10.0;
    private static long TIME_PERIOD_UPDATES = 1000;

    private List<ActivityExtractor> activityExtractors;
    private Timer timer;
    private long lastTimeFeatureSeen;

    public ActivityFP() {
        this.activityExtractors = new Vector<>();
        this.timer = new Timer();
        this.lastTimeFeatureSeen = System.currentTimeMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendPeriodicUpdate();
            }
        }, TIME_PERIOD_UPDATES, TIME_PERIOD_UPDATES);

    }

    @Override
    public void stopFeatureProcessing() {
        this.timer.cancel();
    }

    private void sendPeriodicUpdate() {
        for(ActivityExtractor ae : activityExtractors){
            ae.processFeatureList(getFeatures());
        }
        cleanUpFeatureStack();
    }

    @Override
    protected void cleanUpFeatureStack() {
        super.cleanUpFeatureStack();

        //do an additional periodic cleanup
        if(!getFeatures().isEmpty() && System.currentTimeMillis() - lastTimeFeatureSeen >= (long) (TIME_THRESHOLD*1000)){
            int sizeBefore = getFeatures().size();
            getFeatures().clear();
            Log.d("FEATURE_GC", "Removed " + (sizeBefore - getFeatures().size()) + " features from the stack");

        }
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
            ae.processNewFeature(feature);
        }
        lastTimeFeatureSeen = System.currentTimeMillis();
    }

    @Override
    public double getTimeThresholdForGC() {
        return TIME_THRESHOLD;
    }
}
