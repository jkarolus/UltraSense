package jakobkarolus.de.ultrasense.features.activities;

import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import jakobkarolus.de.ultrasense.features.Feature;
import jakobkarolus.de.ultrasense.features.FeatureProcessor;

/**
 * Combines Features and their respective Activities, by keeping a List of current Features and delegating
 * Activity recognition to its ActivityExtractors.<br>
 * Contrary to the GestureFP, the ActivityFP also informs its AEs regularly about the current feature stack, independent from incoming feature times.
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public class ActivityFP extends FeatureProcessor {

    private static double TIME_THRESHOLD = 10.0;
    private static long TIME_PERIOD_UPDATES = 1000;

    private List<ActivityExtractor> activityExtractors;
    private Timer timer;
    private long lastTimeFeatureSeen;

    /**
     * creates a new ActivityFP and starts periodic updates towards its ActivityExtractors
     */
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

        //only add the feature if it was not consumed
        for(ActivityExtractor ae : activityExtractors){
            if(!ae.processNewFeature(feature, getFeatures())) {
                getFeatures().add(feature);
                Log.i("FEATURE", "" + df.format(feature.getTime()) + ";" + df.format(feature.getLength()) + ";" + df.format(feature.getWeight()));
            }
            else{
                Log.i("FEATURE (CONSUMED)", "" + df.format(feature.getTime()) + ";" + df.format(feature.getLength()) + ";" + df.format(feature.getWeight()));
            }
        }

        printFeaturesOnLog();
        Log.d("FEATURE_STACK", printFeatureStack());

        lastTimeFeatureSeen = System.currentTimeMillis();

    }

    @Override
    public double getTimeThresholdForGC() {
        return TIME_THRESHOLD;
    }
}
