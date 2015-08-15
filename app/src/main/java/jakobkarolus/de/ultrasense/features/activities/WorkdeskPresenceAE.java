package jakobkarolus.de.ultrasense.features.activities;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

/**
 * detects whether the user is currently at the workdesk or away
 *
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public class WorkdeskPresenceAE extends ActivityExtractor{


    //6.077 <-> 7.7479; 0.7506 <-> 1.0188
    //-5.8236 <-> -3.7968; 0.7238 <-> 1.1126

    private static final double WEIGHT_MIN_APPROACH = 4.0;
    private static final double WEIGHT_MAX_APPROACH = 20.0;
    private static final double LENGTH_MIN_APPROACH = 0.5;
    private static final double LENGTH_MAX_APPROACH = 1.3;

    private static final double WEIGHT_MIN_WITHDRAW = -20.0;
    private static final double WEIGHT_MAX_WITHDRAW = -3.0;
    private static final double LENGTH_MIN_WITHDRAW = 0.4;
    private static final double LENGTH_MAX_WITHDRAW = 1.3;

    private static final int COUNTER_THRESHOLD_WORKING = 20;
    private static final int UPDATES_AMOUNT_THRESHOLD = 5;

    private int regularUpdatesCounter;
    private int noFeaturePresentCounter;


    /**
     * creates a new WorkdeskPresence AE. Initial state is DESK_PRESENT
     *
     * @param callback the InferredContextCallback
     */
    public WorkdeskPresenceAE(InferredContextCallback callback) {
        super(callback);
        changeContext(InferredContext.DESK_PRESENT, "Initial state");
        this.noFeaturePresentCounter = 0;
        this.regularUpdatesCounter = 0;

    }


    @Override
    public boolean processNewFeature(Feature f, List<Feature> featureList) {

        regularUpdatesCounter++;

        //we dont want to consume the feature, to get a better time estimate for switching context due to (in-)activity
        if(userIsWithdrawing(f)){
            changeContext(InferredContext.DESK_AWAY, "User withdrawing");
            regularUpdatesCounter = 0;
            return false;
        }

        if(userIsApproaching(f)) {
            changeContext(InferredContext.DESK_PRESENT, "User approaching");
            return false;
        }

        return false;
    }

    @Override
    public void processFeatureList(List<Feature> features) {

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
        if(getCurrentContext() == InferredContext.DESK_PRESENT){
            if(noFeaturePresentCounter >= COUNTER_THRESHOLD_WORKING){
                //change context to DESK_AWAY
                changeContext(InferredContext.DESK_AWAY, "Due to inactivity");
            }
        }

        if(getCurrentContext() == InferredContext.DESK_AWAY){
            if(regularUpdatesCounter >= UPDATES_AMOUNT_THRESHOLD){
                changeContext(InferredContext.DESK_PRESENT, "Activity while not being present");
            }
        }

    }


    private boolean userIsApproaching(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_APPROACH && f.getWeight() <= WEIGHT_MAX_APPROACH && f.getLength() >= LENGTH_MIN_APPROACH && f.getLength() <= LENGTH_MAX_APPROACH;
    }

    private boolean userIsWithdrawing(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_WITHDRAW && f.getWeight() <= WEIGHT_MAX_WITHDRAW && f.getLength() >= LENGTH_MIN_WITHDRAW && f.getLength() <= LENGTH_MAX_WITHDRAW;
    }
}
