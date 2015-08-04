package jakobkarolus.de.pulseradar.features.activities;

import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

    private static final double WEIGHT_MIN_APPROACH = 50;
    private static final double WEIGHT_MAX_APPROACH = 10.0;
    private static final double LENGTH_MIN_APPROACH = 0.5;
    private static final double LENGTH_MAX_APPROACH = 1.15;

    private static final double WEIGHT_MIN_WITHDRAW = -8.0;
    private static final double WEIGHT_MAX_WITHDRAW = -3.0;
    private static final double LENGTH_MIN_WITHDRAW = 0.4;
    private static final double LENGTH_MAX_WITHDRAW = 1.1;

    private static final long TIME_THRESHOLD_WORKING = 60000;
    private static final double TIME_THRESHOLD_LEFT = 5.0;

    private Timer timer;
    private double lastFeatureTimeAfterLeaving;

    private InferredContext currentContext;


    public WorkdeskPresenceAE(InferredContextCallback callback) {
        super(callback);
        this.currentContext = InferredContext.PRESENT;
        this.timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onNoCurrentFeatureDetected();
            }
        }, TIME_THRESHOLD_WORKING);

    }

    private void onNoCurrentFeatureDetected() {
        //change to not being present
        InferredContext oldContext = currentContext;
        currentContext = InferredContext.AWAY;
        getCallback().onInferredContextChange(oldContext, currentContext, "Due to inactivity");

    }

    @Override
    public void processFeatureList(List<Feature> features) {

        if(!features.isEmpty()) {
            Feature f = features.get(features.size()-1);
            //we search for a stretched low/high doppler feature
            if(userIsWithdrawing(f)){
                InferredContext oldContext = currentContext;
                currentContext = InferredContext.AWAY;
                lastFeatureTimeAfterLeaving = f.getTime();
                features.remove(f);
                getCallback().onInferredContextChange(oldContext, currentContext, "User withdrawing " + lastFeatureTimeAfterLeaving);
                timer.cancel();
            }

            else if(userIsApproaching(f)) {
                InferredContext oldContext = currentContext;
                currentContext = InferredContext.PRESENT;
                features.remove(f);

                //there should be regular updates of features!
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        onNoCurrentFeatureDetected();
                    }
                }, TIME_THRESHOLD_WORKING);

                getCallback().onInferredContextChange(oldContext, currentContext, "User approaching");
            }
            else{
                //some other new feature, check if we are at the workdesk
                if(currentContext == InferredContext.AWAY){
                    Log.i("TIME", "f:" + f.getTime() + ", last: " + lastFeatureTimeAfterLeaving + ", diff: " + (f.getTime()-lastFeatureTimeAfterLeaving));
                    if((f.getTime() - lastFeatureTimeAfterLeaving) >= TIME_THRESHOLD_LEFT){
                        //we registered a feature, although we are not present
                        InferredContext oldContext = currentContext;
                        currentContext = InferredContext.PRESENT;
                        lastFeatureTimeAfterLeaving = f.getTime();
                        getCallback().onInferredContextChange(oldContext, currentContext, "Activity while not being present");

                        //there should be regular updates of features!
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                onNoCurrentFeatureDetected();
                            }
                        }, TIME_THRESHOLD_WORKING);
                    }
                }
                if(currentContext == InferredContext.PRESENT){
                    //update the time for it to be correct when falling back to not being present
                    lastFeatureTimeAfterLeaving = f.getTime();

                    //update the timer
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            onNoCurrentFeatureDetected();
                        }
                    }, TIME_THRESHOLD_WORKING);
                }
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
