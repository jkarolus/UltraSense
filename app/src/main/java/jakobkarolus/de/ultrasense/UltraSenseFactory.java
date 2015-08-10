package jakobkarolus.de.ultrasense;

import android.app.Activity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.ultrasense.algorithm.AlgoHelper;
import jakobkarolus.de.ultrasense.algorithm.CWSignalGenerator;
import jakobkarolus.de.ultrasense.audio.AudioManager;
import jakobkarolus.de.ultrasense.features.activities.ActivityFP;
import jakobkarolus.de.ultrasense.features.FeatureDetector;
import jakobkarolus.de.ultrasense.features.GaussianFE;
import jakobkarolus.de.ultrasense.features.gestures.GestureFP;
import jakobkarolus.de.ultrasense.features.MeanBasedFD;
import jakobkarolus.de.ultrasense.features.activities.BedFallAE;
import jakobkarolus.de.ultrasense.features.activities.InferredContextCallback;
import jakobkarolus.de.ultrasense.features.activities.WorkdeskPresenceAE;
import jakobkarolus.de.ultrasense.features.gestures.DownUpGE;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;
import jakobkarolus.de.ultrasense.features.gestures.GestureExtractor;
import jakobkarolus.de.ultrasense.features.gestures.SwipeGE;

/**
 * Factory class for creating different UltraSense scenarios
 *
 * <br><br>
 * Created by Jakob on 10.08.2015.
 */
public class UltraSenseFactory {

    public static final double SAMPLE_RATE = 44100.0;
    private static final int fftLength = 4096;
    private static final int hopSize = 2048;
    private static double frequency = 20000;


    private AudioManager audioManager;
    private GestureFP gestureFP;
    private ActivityFP activityFP;
    private FeatureDetector featureDetector;
    private Activity activity;
    private boolean initialized;

    public UltraSenseFactory(Activity activity){
        this.activity = activity;
        this.audioManager = new AudioManager(activity);
        this.initialized = false;
    }


    public void createGestureDetector(GestureCallback callback, boolean noisy, boolean usePreCalibration){

        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, 0.1, 1.0, SAMPLE_RATE));

        if(noisy)
            featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, frequency, 3, -50.0, 3, 2, 1, AlgoHelper.getHannWindow(fftLength), true, 15);
        else
            featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, frequency, 4, -55.0, 3, 2, 0, AlgoHelper.getHannWindow(fftLength), false, 0.0);


        gestureFP = new GestureFP(callback);
        List<GestureExtractor> gestureExtractors = new Vector<>();
        gestureExtractors.add(new DownUpGE());
        gestureExtractors.add(new SwipeGE());

        for (GestureExtractor ge : gestureExtractors) {
            if (!usePreCalibration)
                initializeGEThresholds(ge);
            gestureFP.registerGestureExtractor(ge);
        }
        featureDetector.registerFeatureExtractor(new GaussianFE(gestureFP));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;
    }

    public void createWorkdeskPresenceDetector(InferredContextCallback callback){

        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, 0.1, 1.0, SAMPLE_RATE));
        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, frequency, 5, -60.0, 1.0, 0.5, 10, AlgoHelper.getHannWindow(fftLength), true, 8.0);

        activityFP = new ActivityFP();
        activityFP.registerActivityExtractor(new WorkdeskPresenceAE(callback));
        featureDetector.registerFeatureExtractor(new GaussianFE(activityFP));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;

    }

    public void createBedFallDetector(InferredContextCallback callback){

        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, 0.1, 1.0, SAMPLE_RATE));
        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, frequency, 5, -60.0, 2.0, 1.0, 5, AlgoHelper.getHannWindow(fftLength), true, 20.0);

        activityFP = new ActivityFP();
        activityFP.registerActivityExtractor(new BedFallAE(callback));
        featureDetector.registerFeatureExtractor(new GaussianFE(activityFP));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;

    }

    public void startDetection() throws IllegalStateException{
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before starting any detection!");

        audioManager.startDetection();
    }

    public void stopDetection() throws IllegalStateException{
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before stoping any detection!");

        audioManager.stopDetection();
        if(activityFP != null)
            activityFP.stopFeatureProcessing();
    }

    private boolean initializeGEThresholds(GestureExtractor ge) {
        try {
            ObjectInputStream in = new ObjectInputStream(activity.openFileInput(ge.getName() + ".calib"));
            Map<String, Double> thresholds = (HashMap<String, Double>) in.readObject();
            return ge.setThresholds(thresholds);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


}
