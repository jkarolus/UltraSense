package jakobkarolus.de.ultrasense.features.gestures;

import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import jakobkarolus.de.ultrasense.features.Feature;
import jakobkarolus.de.ultrasense.features.FeatureProcessor;

/**
 * Combines Features and their respective Gesture, by keeping a List of current Feature and delegating
 * Gesture recognition to its GestureExtractors
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class GestureFP extends FeatureProcessor {

    private static final double TIME_THRESHOLD = 5.0;
    private static final int MAX_CALIBRATION_RUNS = 5;

    private List<GestureExtractor> gestureExtractors;
    private GestureCallback gestureCallback;
    private int calibrationRuns;
    private boolean isCalibrating;
    private GestureExtractor calibrator;

    public GestureFP(GestureCallback gestureCallback){
        this.gestureCallback = gestureCallback;
        gestureExtractors = new Vector<>();
        isCalibrating = false;
        calibrationRuns = 0;
    }

    public void registerGestureExtractor(GestureExtractor ge){
        this.gestureExtractors.add(ge);
    }

    public void unregisterGestureExtractor(GestureExtractor ge){
        this.gestureExtractors.remove(ge);
    }

    public void unregisterAllGestureExtractors(){
        this.gestureExtractors.clear();
    }

    public List<GestureExtractor> getGestureExtractors(){
        return this.gestureExtractors;
    }

    public GestureExtractor getCalibrator(){
        return this.calibrator;
    }

    public String[] getGestureExtractorNames(){
        String[] names = new String[gestureExtractors.size()];
        for(int i=0; i < gestureExtractors.size(); i++)
            names[i] = gestureExtractors.get(i).getName();
        return names;
    }

    public void startCalibrating(GestureExtractor ge){
        this.calibrator = ge;
        this.calibrator.resetThresholds();
        calibrationRuns = 0;
        isCalibrating = true;
    }


    @Override
    public void stopFeatureProcessing() {
        //GestureFP does not implicit periodic feature processing -> nothing to do here
    }

    @Override
    public void processFeatureOnSubclass(Feature feature) {

        getFeatures().add(feature);
        Collections.sort(getFeatures(), new Comparator<Feature>() {
            @Override
            public int compare(Feature lhs, Feature rhs) {
                if (lhs.getTime() > rhs.getTime())
                    return 1;
                else
                    return -1;
            }
        });


        printFeaturesOnLog();
        Log.d("FEATURE_STACK", printFeatureStack());

        //calibration ongoing
        if(isCalibrating && calibrator != null){

            //it can happen that we have another feature "in-line" waiting (e.g. a small up motion after a down swing)
            //-> we gotta ignore this, or it will lead to an unsuccessful calibration step irritating the user
            //TODO
            /*
            if((feature.getTime() - currentFeatureTime) < CALIBRATION_TIME_THRESHOLD || !(Math.abs(currentFeatureTime - 0.0) < 1e-6)) {
                features.clear();
                return;
            }
            */

            if(calibrationRuns < MAX_CALIBRATION_RUNS){
                CalibrationState calibState = calibrator.calibrate(getFeatures());

                if(calibState == CalibrationState.SUCCESSFUL) {
                    calibrationRuns++;
                }
                if(calibrationRuns < MAX_CALIBRATION_RUNS)
                    gestureCallback.onCalibrationStep(calibState);

            }
            else{
                //calibration finished
                gestureCallback.onCalibrationFinished(calibrator.getThresholdMap(), calibrator.getThresholds(), calibrator.getName());
                isCalibrating = false;
            }
        }

        //normal detection
        else {
            for (GestureExtractor ge : gestureExtractors) {
                final List<Gesture> gestures = ge.detectGesture(getFeatures());
                for (final Gesture g : gestures) {
                    Log.i("GESTURE", g.toString());
                    gestureCallback.onGestureDetected(g);
                }
            }
            cleanUpFeatureStack();
        }
    }

    @Override
    public double getTimeThresholdForGC() {
        return TIME_THRESHOLD;
    }
}
