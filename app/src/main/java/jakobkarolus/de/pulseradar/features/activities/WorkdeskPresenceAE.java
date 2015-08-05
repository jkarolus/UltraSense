package jakobkarolus.de.pulseradar.features.activities;

import android.util.Log;

import java.util.List;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * detects whether the user is currently at the workdesk or away
 *
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public class WorkdeskPresenceAE extends ActivityExtractor{


    //6.077 <-> 7.7479; 0.7506 <-> 1.0188
    //-5.8236 <-> -3.7968; 0.7238 <-> 1.1126

    private static final double WEIGHT_MIN_APPROACH = 5.0;
    private static final double WEIGHT_MAX_APPROACH = 10.0;
    private static final double LENGTH_MIN_APPROACH = 0.5;
    private static final double LENGTH_MAX_APPROACH = 1.15;

    private static final double WEIGHT_MIN_WITHDRAW = -8.0;
    private static final double WEIGHT_MAX_WITHDRAW = -3.0;
    private static final double LENGTH_MIN_WITHDRAW = 0.4;
    private static final double LENGTH_MAX_WITHDRAW = 1.1;

    private static final int COUNTER_THRESHOLD_WORKING = 20;
    private static final int UPDATES_AMOUNT_THRESHOLD = 5;

    private int regularUpdatesCounter;
    private int noFeaturePresentCounter;

    private InferredContext currentContext;


    public WorkdeskPresenceAE(InferredContextCallback callback) {
        super(callback);
        this.currentContext = InferredContext.PRESENT;
        this.noFeaturePresentCounter = 0;
        this.regularUpdatesCounter = 0;

    }


    @Override
    public boolean processNewFeature(Feature f) {

        regularUpdatesCounter++;

        if(userIsWithdrawing(f)){
            InferredContext oldContext = currentContext;
            currentContext = InferredContext.AWAY;
            getCallback().onInferredContextChange(oldContext, currentContext, "User withdrawing ");
            regularUpdatesCounter = 0;
            return true;
        }

        if(userIsApproaching(f)) {
            InferredContext oldContext = currentContext;
            currentContext = InferredContext.PRESENT;
            getCallback().onInferredContextChange(oldContext, currentContext, "User approaching");
            return true;
        }

        return false;
    }

    @Override
    public void processFeatureList(List<Feature> features) {

        Log.w("SIZE", "" + features.size());
        Log.w("NO_FEATURE", "" + noFeaturePresentCounter);

        //update the state according to the feature list
        if(features.isEmpty()){
            //there have been no updates since at least 10 seconds
            regularUpdatesCounter=0;
            noFeaturePresentCounter++;

        }
        else{
            //there are features present (no older than 10 seconds)
            noFeaturePresentCounter = 0;
        }

        //decide whether to change context based on (in-)activity
        if(currentContext == InferredContext.PRESENT){
            if(noFeaturePresentCounter >= COUNTER_THRESHOLD_WORKING){
                //change context to AWAY
                InferredContext oldContext = currentContext;
                currentContext = InferredContext.AWAY;
                getCallback().onInferredContextChange(oldContext, currentContext, "Due to inactivity");
            }
        }

        if(currentContext == InferredContext.AWAY){
            if(regularUpdatesCounter >= UPDATES_AMOUNT_THRESHOLD){
                InferredContext oldContext = currentContext;
                currentContext = InferredContext.PRESENT;
                getCallback().onInferredContextChange(oldContext, currentContext, "Activity while not being present");
            }
        }

    }


    private boolean userIsApproaching(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_APPROACH && f.getWeight() <= WEIGHT_MAX_APPROACH && f.getLength() >= LENGTH_MIN_APPROACH && f.getLength() <= LENGTH_MAX_APPROACH;
    }

    private boolean userIsWithdrawing(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_WITHDRAW && f.getWeight() <= WEIGHT_MAX_WITHDRAW && f.getLength() >= LENGTH_MIN_WITHDRAW && f.getLength() <= LENGTH_MAX_WITHDRAW;
    }

    @Override
    public InferredContext getCurrentContext() {
        return currentContext;
    }
}
