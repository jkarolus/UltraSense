package jakobkarolus.de.ultrasense.features.activities;

import android.util.Log;

import java.util.List;

import jakobkarolus.de.ultrasense.features.Feature;

import static jakobkarolus.de.ultrasense.features.activities.InferredContext.*;

/**
 * Analyzes the state of the user during sleep and detects emergency like falls
 *
 * <br><br>
 * Created by Jakob on 06.08.2015.
 */
public class BedFallAE extends ActivityExtractor {

    private static double WEIGHT_MIN_APPROACH = 5.0;
    private static double WEIGHT_MAX_APPROACH = 15.0;
    private static double LENGTH_MIN_APPROACH = 0.4;
    private static double LENGTH_MAX_APPROACH = 1.4;

    private static double WEIGHT_MIN_WITHDRAW = -15.0;
    private static double WEIGHT_MAX_WITHDRAW = -4.0;
    private static double LENGTH_MIN_WITHDRAW = 0.37;
    private static double LENGTH_MAX_WITHDRAW = 1.5;

    private static double WEIGHT_MIN_FALL = -15.0;
    private static double WEIGHT_MAX_FALL = -3.8;
    private static double LENGTH_MIN_FALL = 0.1;
    private static double LENGTH_MAX_FALL = 0.37;

    private static double WEIGHT_MIN_POTENTIAL_FALL = -3.8;
    private static double WEIGHT_MAX_POTENTIAL_FALL = -1.6;
    private static double LENGTH_MIN_POTENTIAL_FALL = 0.1;
    private static double LENGTH_MAX_POTENTIAL_FALL = 0.4;

    private static double WEIGHT_MIN_BED_MOVING = -1.5;
    private static double WEIGHT_MAX_BED_MOVING = 1.5;
    private static double LENGTH_MIN_BED_MOVING = 0.015;
    private static double LENGTH_MAX_BED_MOVING = 0.24;

    /**
     * user is awake -> overlaps with withdraw and fall, though the context needs to be
     * AWAKE to trigger a withdraw or fall
     */
    //TODO: not used anymore
    private static double WEIGHT_MIN_BED_AWAKE_HIGH = 1.5;
    private static double WEIGHT_MAX_BED_AWAKE_HIGH = 7.0;
    private static double LENGTH_MIN_BED_AWAKE_HIGH = 0.24;
    private static double LENGTH_MAX_BED_AWAKE_HIGH = 0.5;

    private static double WEIGHT_MIN_BED_AWAKE_LOW = -7.0;
    private static double WEIGHT_MAX_BED_AWAKE_LOW = -1.5;
    private static double LENGTH_MIN_BED_AWAKE_LOW = 0.24;
    private static double LENGTH_MAX_BED_AWAKE_LOW = 0.5;

    private static int FALL_IMMUNITY_TIME = 5;
    private static double POTENTIAL_FALL_THRESHOLD_TIME = 0.3;
    private int currentFallImmunityTime;
    private double potentialFallTime;

    /**
     * creates a new BedFall AE. Initial state is BED_PRESENT
     *
     * @param callback the InferredContextCallback
     */
    public BedFallAE(InferredContextCallback callback) {
        super(callback);
        changeContext(BED_PRESENT, "Initial state");
        currentFallImmunityTime = FALL_IMMUNITY_TIME;
    }

    @Override
    public boolean processNewFeature(Feature f, List<Feature> featureList) {


        if((getCurrentContext() == BED_AWAY || getCurrentContext() == BED_POTENTIAL_FALL) && userIsApproaching(f)) {
            changeContext(BED_PRESENT, "User approaching");
            currentFallImmunityTime = FALL_IMMUNITY_TIME;
            return true;
        }


        //make sure the status is updated for the other detection parts to work correctly
        if(getCurrentContext() == BED_POTENTIAL_FALL){
            //we got a new feature, check if is at least 1 second after the first potential fall
            if((f.getTime() - potentialFallTime) >= POTENTIAL_FALL_THRESHOLD_TIME)
                changeContext(BED_PRESENT, "User present. No fall!");
        }


        if(getCurrentContext() == BED_PRESENT){
           //check if it has been some time since the user arrived, this prevents accidental fall/withdraw prediction
            if(currentFallImmunityTime <= 0) {

                if(userIsWithdrawing(f)){
                    changeContext(BED_AWAY, "User walking away");
                    return true;
                }

                if (userHasFallen(f)) {
                    changeContext(BED_FALL, "Fall detected.");
                    return true;
                }
                //gettings upright and falling lighter is similar
                if(potentialFallDetected(f)){
                    changeContext(BED_POTENTIAL_FALL, "Potential fall detected. Waiting for features.");
                    potentialFallTime = f.getTime();
                    //we need at least one feature on the stack -> dont consume this one
                    return false;
                }
            }
        }

        return false;

    }

    private boolean potentialFallDetected(Feature f) {
        return f.getWeight() > WEIGHT_MIN_POTENTIAL_FALL && f.getWeight() < WEIGHT_MAX_POTENTIAL_FALL && f.getLength() > LENGTH_MIN_POTENTIAL_FALL && f.getLength() < LENGTH_MAX_POTENTIAL_FALL;
    }

    private boolean userIsApproaching(Feature f) {
        return f.getWeight() > WEIGHT_MIN_APPROACH && f.getWeight() < WEIGHT_MAX_APPROACH && f.getLength() > LENGTH_MIN_APPROACH && f.getLength() < LENGTH_MAX_APPROACH;

    }

    private boolean userHasFallen(Feature f) {
        return f.getWeight() >= WEIGHT_MIN_FALL && f.getWeight() <= WEIGHT_MAX_FALL && f.getLength() >= LENGTH_MIN_FALL && f.getLength() <= LENGTH_MAX_FALL;
    }

    private boolean userIsWithdrawing(Feature f) {
        return f.getWeight() > WEIGHT_MIN_WITHDRAW && f.getWeight() < WEIGHT_MAX_WITHDRAW && f.getLength() > LENGTH_MIN_WITHDRAW && f.getLength() < LENGTH_MAX_WITHDRAW;

    }

    @Override
    public void processFeatureList(List<Feature> features) {

        switch(getCurrentContext()){
            case BED_POTENTIAL_FALL:
                checkIfFallOrNot(features);
                break;
            case BED_PRESENT:
                int old = currentFallImmunityTime;
                currentFallImmunityTime = Math.max(0, currentFallImmunityTime-1);
                if(old == 1)
                    Log.e("IMM_TIME", "No Immunity!");
                break;
        }

    }

    private void checkIfFallOrNot(List<Feature> features) {
        if(features.isEmpty()){
            //it has been 10 seconds since the last potential fall feature
            changeContext(BED_FALL, "User has fallen (based on potential fall)!");
        }
        else{
            //new events will be caught in processNewFeature -> so there has been none
            //but is also has not been 10 seconds since the potential fall
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
