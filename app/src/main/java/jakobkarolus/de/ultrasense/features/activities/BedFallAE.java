package jakobkarolus.de.ultrasense.features.activities;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

import static jakobkarolus.de.ultrasense.features.activities.InferredContext.*;

/**
 * Analyzes the state of the user during sleep and detect emergency like falls
 *
 * <br><br>
 * Created by Jakob on 06.08.2015.
 */
public class BedFallAE extends ActivityExtractor {


    /**
     * does not include bed_rect_1-3 -> unlikely scenario
     */
    private static double WEIGHT_MIN_WITHDRAW = -10.0;
    private static double WEIGHT_MAX_WITHDRAW = -5.0;
    private static double LENGTH_MIN_WITHDRAW = 0.5;
    private static double LENGTH_MAX_WITHDRAW = 1.2;

    /**
     * includes both soft and hard falls
     */
    private static double WEIGHT_MIN_FALL = -8.0;
    private static double WEIGHT_MAX_FALL = -3.3;
    private static double LENGTH_MIN_FALL = 0.14;
    private static double LENGTH_MAX_FALL = 0.48;

    /**
     * does include appr_rect_1-2
     */
    private static double WEIGHT_MIN_APPROACH = 8.0;
    private static double WEIGHT_MAX_APPROACH = 15;
    private static double LENGTH_MIN_APPROACH = 0.5;
    private static double LENGTH_MAX_APPROACH = 1.4;

    /**
     * from bed moving
     */
    private static double WEIGHT_MIN_BED_MOVING = -1.5;
    private static double WEIGHT_MAX_BED_MOVING = 1.5;
    private static double LENGTH_MIN_BED_MOVING = 0.015;
    private static double LENGTH_MAX_BED_MOVING = 0.24;

    /**
     * user is awake -> overlaps with withdraw and fall, though the context needs to be
     * AWAKE to trigger a withdraw or fall
     */
    private static double WEIGHT_MIN_BED_AWAKE_HIGH = 1.5;
    private static double WEIGHT_MAX_BED_AWAKE_HIGH = 7.0;
    private static double LENGTH_MIN_BED_AWAKE_HIGH = 0.24;
    private static double LENGTH_MAX_BED_AWAKE_HIGH = 0.5;

    private static double WEIGHT_MIN_BED_AWAKE_LOW = -7.0;
    private static double WEIGHT_MAX_BED_AWAKE_LOW = -1.5;
    private static double LENGTH_MIN_BED_AWAKE_LOW = 0.24;
    private static double LENGTH_MAX_BED_AWAKE_LOW = 0.5;


    public BedFallAE(InferredContextCallback callback) {
        super(callback);
        changeContext(BED_PRESENT, "Initial state");
    }

    @Override
    public boolean processNewFeature(Feature feature, List<Feature> featureList) {

        processFeatureList(featureList);

        if(userIsWithdrawing(feature) && getCurrentContext() == BED_AWAKE){
            changeContext(BED_AWAY, "User walking away");
            return true;
        }

        if(userHasFallen(feature) && getCurrentContext() == BED_AWAKE){
            changeContext(BED_FALL, "User has fallen");
            return true;
        }

        if(userIsApproaching(feature) && getCurrentContext() == BED_AWAY){
            changeContext(BED_PRESENT, "User approaching");
            return true;
        }

        return false;

    }

    private boolean userIsApproaching(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_APPROACH && f.getWeight() <= WEIGHT_MAX_APPROACH && f.getLength() >= LENGTH_MIN_APPROACH && f.getLength() <= LENGTH_MAX_APPROACH;

    }

    private boolean userHasFallen(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_FALL && f.getWeight() <= WEIGHT_MAX_FALL && f.getLength() >= LENGTH_MIN_FALL && f.getLength() <= LENGTH_MAX_FALL;

    }

    private boolean userIsWithdrawing(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_WITHDRAW && f.getWeight() <= WEIGHT_MAX_WITHDRAW && f.getLength() >= LENGTH_MIN_WITHDRAW && f.getLength() <= LENGTH_MAX_WITHDRAW;

    }

    @Override
    public void processFeatureList(List<Feature> features) {

        switch(getCurrentContext()){
            case BED_PRESENT:
                checkIfTheUserIsSleepingOrAwake(features);
                break;
            case BED_SLEEPING:
                checkIfTheUserHasWokenUp(features);
                break;
            case BED_AWAY:
                checkIfUserIsPresent(features);
                break;
            case BED_AWAKE:
                checkIfTheUserSleepsAgain(features);
                break;
        }

    }

    private void checkIfTheUserSleepsAgain(List<Feature> features) {
        if(features.isEmpty()){
            //no new features in the last 10 seconds -> user sleeping
            changeContext(BED_SLEEPING, "No features detected");
        }
        else {
            //check if we only have "bed movement features"
            if (userIsMovingInBed(features))
                changeContext(BED_SLEEPING, "Moving in Bed");
        }

        //otherwise the state stays AWAKE
    }

    private void checkIfUserIsPresent(List<Feature> features) {

        //TODO: implement
    }

    private void checkIfTheUserHasWokenUp(List<Feature> features) {
        if(userIsAwake(features))
            changeContext(BED_AWAKE, "Movement suggests user is awake");
    }

    private void checkIfTheUserIsSleepingOrAwake(List<Feature> features) {
        //the user was recognized as being present
        if(features.isEmpty()){
            //no new features in the last 10 seconds -> user sleeping
            changeContext(BED_SLEEPING, "No features detected");
        }
        else{
            //check if we only have "bed movement features"
            if(userIsMovingInBed(features))
                changeContext(BED_SLEEPING, "Moving in Bed");

            //check if at least one feature is "big" enough for it to indicate that the user is awake
            if(userIsAwake(features))
                changeContext(BED_AWAKE, "Movement suggests user is awake");
        }
    }

    private boolean userIsAwake(List<Feature> features) {
        for(Feature f : features){
            if(f.getWeight() > WEIGHT_MIN_BED_AWAKE_HIGH && f.getWeight() < WEIGHT_MAX_BED_AWAKE_HIGH && f.getLength() > LENGTH_MIN_BED_AWAKE_HIGH && f.getLength() < LENGTH_MAX_BED_AWAKE_HIGH ||
                    f.getWeight() > WEIGHT_MIN_BED_AWAKE_LOW && f.getWeight() < WEIGHT_MAX_BED_AWAKE_LOW && f.getLength() > LENGTH_MIN_BED_AWAKE_LOW && f.getLength() < LENGTH_MAX_BED_AWAKE_LOW)
                return true;
        }
        return false;
    }

    private boolean userIsMovingInBed(List<Feature> features) {
        for(Feature f : features){
            if(!(f.getWeight() >= WEIGHT_MIN_BED_MOVING && f.getWeight() <= WEIGHT_MAX_BED_MOVING && f.getLength() >= LENGTH_MIN_BED_MOVING && f.getLength() <= LENGTH_MAX_BED_MOVING))
                return false;
        }
        return true;
    }
}
