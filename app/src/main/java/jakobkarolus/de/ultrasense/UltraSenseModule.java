package jakobkarolus.de.ultrasense;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jakobkarolus.de.ultrasense.algorithm.AlgoHelper;
import jakobkarolus.de.ultrasense.audio.AudioManager;
import jakobkarolus.de.ultrasense.audio.CWSignalGenerator;
import jakobkarolus.de.ultrasense.audio.FMCWSignalGenerator;
import jakobkarolus.de.ultrasense.audio.SignalGenerator;
import jakobkarolus.de.ultrasense.features.DummyFeatureDetector;
import jakobkarolus.de.ultrasense.features.DummyFeatureProcessor;
import jakobkarolus.de.ultrasense.features.FeatureDetector;
import jakobkarolus.de.ultrasense.features.FeatureProcessor;
import jakobkarolus.de.ultrasense.features.GaussianFE;
import jakobkarolus.de.ultrasense.features.MeanBasedFD;
import jakobkarolus.de.ultrasense.features.activities.ActivityFP;
import jakobkarolus.de.ultrasense.features.activities.BedFallAE;
import jakobkarolus.de.ultrasense.features.activities.InferredContextCallback;
import jakobkarolus.de.ultrasense.features.activities.WorkdeskPresenceAE;
import jakobkarolus.de.ultrasense.features.gestures.DownUpGE;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;
import jakobkarolus.de.ultrasense.features.gestures.GestureExtractor;
import jakobkarolus.de.ultrasense.features.gestures.GestureFP;
import jakobkarolus.de.ultrasense.features.gestures.SwipeGE;
import jakobkarolus.de.ultrasense.view.SettingsFragment;

/**
 * Factory class for creating different UltraSense scenarios
 *
 * <br><br>
 * Created by Jakob on 10.08.2015.
 */
public class UltraSenseModule {

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


    /**
     * creates a new UltraSenseModule, initializing the AudioManager
     * @param activity the Activity to associate this module with
     */
    public UltraSenseModule(Activity activity){
        this.activity = activity;
        this.audioManager = new AudioManager(activity);
        this.initialized = false;
    }

    /**
     * creates a custom UltraSense scenario given the settings provided by the user.<br>
     * Use when experimenting with CW/FMCW in conjunction with the Recording function
     *
     * @param settingsParameters the settings defined by the user (settings tab)
     * @param gestureCallback the GestureCallback to use if specified (pass null if no GE was selected)
     * @param inferredContextCallback the InferredContextCallback to use if specified (pass null if no AE was selected)
     * @throws IllegalArgumentException
     */
    public void createCustomScenario(SharedPreferences settingsParameters, GestureCallback gestureCallback, InferredContextCallback inferredContextCallback) throws IllegalArgumentException{
        resetState();
        SignalGenerator signalGen;
        String mode = settingsParameters.getString(SettingsFragment.PREF_MODE, "CW");
        try {
            if(mode.equals(SettingsFragment.FMCW_MODE)) {
                double botFreq = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_FMCW_BOT_FREQ, ""));
                double topFreq = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_FMCW_TOP_FREQ, ""));
                double chirpDur = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_FMCW_CHIRP_DUR, ""));
                double chirpCycles = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_FMCW_CHIRP_CYCLES, ""));
                boolean rampUp = settingsParameters.getBoolean(SettingsFragment.KEY_FMCW_RAMP_UP, false);

                signalGen =  new FMCWSignalGenerator(topFreq, botFreq, chirpDur, chirpCycles, SAMPLE_RATE, 1.0f, !rampUp);
            }
            else {
                double freq = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_CW_FREQ, ""));
                signalGen =  new CWSignalGenerator(freq, SAMPLE_RATE);
            }
        }catch (NumberFormatException e) {
            throw new IllegalArgumentException("Specified FMCW Parameters are not valid!", e);
        }

        try{
            if(mode.equals(SettingsFragment.CW_MODE)){
                int fftLength = Integer.parseInt(settingsParameters.getString(SettingsFragment.KEY_FFT_LENGTH, ""));
                double hopSizeFraction = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_HOPSIZE, ""));
                int hopSize = (int) (hopSizeFraction * fftLength);
                int halfCarrierWidth = Integer.parseInt(settingsParameters.getString(SettingsFragment.KEY_HALF_CARRIER_WIDTH, ""));
                double dbThreshold = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_DB_THRESHOLD, ""));
                double highFeatureThr = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_HIGH_FEAT_THRESHOLD, ""));
                double lowFeatureThr = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_LOW_FEAT_THRESHOLD, ""));
                int slackWidth = Integer.parseInt(settingsParameters.getString(SettingsFragment.KEY_FEAT_SLACK, ""));
                double freq = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_CW_FREQ, ""));
                boolean ignoreNoise = settingsParameters.getBoolean(SettingsFragment.KEY_CW_IGNORE_NOISE, false);
                double maxFeatureThreshold = Double.parseDouble(settingsParameters.getString(SettingsFragment.KEY_CW_MAX_FEAT_THRESHOLD, ""));
                boolean usePreCalibration = settingsParameters.getBoolean(SettingsFragment.KEY_USE_PRECALIBRATION, true);
                boolean noisy = settingsParameters.getBoolean(SettingsFragment.KEY_CW_NOISY_ENV, false);

                FeatureProcessor fp = initializeFPForCustomScenario(settingsParameters.getString(SettingsFragment.KEY_CW_EXTRACTORS, "0"), gestureCallback, inferredContextCallback, usePreCalibration, noisy);
                featureDetector = new MeanBasedFD(fp, SAMPLE_RATE, fftLength, hopSize, freq, halfCarrierWidth, dbThreshold, highFeatureThr, lowFeatureThr, slackWidth, AlgoHelper.getHannWindow(fftLength), ignoreNoise, maxFeatureThreshold);
            }
            else
                featureDetector = new DummyFeatureDetector(0.0);

        }catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"Specified CW or Feature detection parameters are not valid!", e);
        }

        audioManager.setSignalGenerator(signalGen);
        featureDetector.registerFeatureExtractor(new GaussianFE(0));

        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;
    }

    /**
     * creates an UltraSense GestureDetection scenario.<br>
     * Parameters for feature detection will be set accordingly. However the user can choose the environment noise and
     * whether to use his calibration values for feature extraction (from a previous calibration) or precalibrated parameters
     *
     * @param callback the GestureCallback to call when detecting a gesture
     * @param noisy whether the environment is noisy (in other words: whether detection should compensate for that)
     * @param usePreCalibration whether to use precalibrated parameters for feature extraction specified by the developer
     */
    public void createGestureDetector(GestureCallback callback, boolean noisy, boolean usePreCalibration){
        resetState();
        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, SAMPLE_RATE));

        gestureFP = new GestureFP(callback);
        List<GestureExtractor> gestureExtractors = new Vector<>();
        gestureExtractors.add(new DownUpGE());
        gestureExtractors.add(new SwipeGE());

        for (GestureExtractor ge : gestureExtractors) {
            if (!usePreCalibration)
                initializeGEThresholds(ge, noisy);
            gestureFP.registerGestureExtractor(ge);
        }

        //silent S3: -55, 6 (hcw), 4,3

        if(noisy)
            featureDetector = new MeanBasedFD(gestureFP, SAMPLE_RATE, fftLength, hopSize, frequency, 3, -50.0, 3, 2, 1, AlgoHelper.getHannWindow(fftLength), true, 15);
        else
            featureDetector = new MeanBasedFD(gestureFP, SAMPLE_RATE, fftLength, hopSize, frequency, 4, -55.0, 3, 2, 0, AlgoHelper.getHannWindow(fftLength), false, 0.0);



        featureDetector.registerFeatureExtractor(new GaussianFE(0));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;
    }

    /**
     * creates an UltraSense WorkdeskPresence ActivityDetector.<br>
     * Cycles through different context state, while the user works at his desk, e.g. recognizes if the user leaves/ is absent
     *
     * @param callback the InferredContextCallback to inform about context changes
     */
    public void createWorkdeskPresenceDetector(InferredContextCallback callback){
        resetState();
        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, SAMPLE_RATE));

        activityFP = new ActivityFP();
        activityFP.registerActivityExtractor(new WorkdeskPresenceAE(callback));

        featureDetector = new MeanBasedFD(activityFP, SAMPLE_RATE, fftLength, hopSize, frequency, 5, -60.0, 1.0, 0.5, 10, AlgoHelper.getHannWindow(fftLength), true, 8.0);

        featureDetector.registerFeatureExtractor(new GaussianFE(0));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;

    }

    /**
     * creates an UltraSense BedFall ActivityDetector.<br>
     * Cycles through different context state, while the use is sleeping in a bed. E.g. being awake/sleeping or moving away from the bed.<br>
     * Can detect an emergency like fall when moving away from the bed.
     *
     * @param callback the InferredContextCallback to inform about context changes
     */
    public void createBedFallDetector(InferredContextCallback callback){
        resetState();
        audioManager.setSignalGenerator(new CWSignalGenerator(frequency, SAMPLE_RATE));

        activityFP = new ActivityFP();
        activityFP.registerActivityExtractor(new BedFallAE(callback));

        featureDetector = new MeanBasedFD(activityFP, SAMPLE_RATE, fftLength, hopSize, frequency, 5, -60.0, 3.0, 1.5, 5, AlgoHelper.getHannWindow(fftLength), true, 15.0);


        featureDetector.registerFeatureExtractor(new GaussianFE(0));
        audioManager.setFeatureDetector(featureDetector);
        this.initialized = true;

    }

    /**
     * starts a previously created scenario.<br>
     * DOES NOT record to a file! Use startRecord() for this.
     *
     * @throws IllegalStateException if no scenario was created
     */
    public void startDetection() throws IllegalStateException{
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before starting any detection!");

        audioManager.startDetection();
    }

    /**
     * stops a previously started scenario. Subsequent calls will do nothing.<br>
     * Use in conjunction with startDetection()
     *
     * @throws IllegalStateException if no scenario was created
     */
    public void stopDetection() throws IllegalStateException{
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before stoping any detection!");

        audioManager.stopDetection();
        if(activityFP != null)
            activityFP.stopFeatureProcessing();
    }

    private boolean initializeGEThresholds(GestureExtractor ge, boolean noisy) {
        try {
            ObjectInputStream in = new ObjectInputStream(activity.openFileInput(ge.getName() + (noisy ? "_noisy" : "") + ".calib"));
            Map<String, Double> thresholds = (HashMap<String, Double>) in.readObject();
            return ge.setThresholds(thresholds);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * grants access to the ActivityFP if the scenario created it
     * @return the current ActivityFP if created
     */
    public ActivityFP getActivityFP() {
        return activityFP;
    }

    /**
     * grants access to the GestureFP if the scenario created it
     * @return the current GestureFP if created
     */
    public GestureFP getGestureFP() {
        return gestureFP;
    }

    /**
     * grants access to the AudioManager<br>
     * E.g. to access record data before saving it to a file
     *
     * @return the AudioManager associated with the current scenario
     */
    public AudioManager getAudioManager() {
        return audioManager;
    }


    /**
     *
     * @return a String represenation of the feature detection parameters
     */
    public String printFeatureDetectionParameters(){
        if(featureDetector != null)
            return featureDetector.printParameters();
        else
            return "";
    }

    /**
     * starts a previously created scenario.<br>
     * DOES record to a file! Can be used for later analysis.
     *
     * @throws IllegalStateException if no scenario was created
     */
    public void startRecord() {
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before starting any detection!");

        try {
            audioManager.startRecord();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * stops a previously started scenario. Subsequent calls will do nothing.<br>
     * Use in conjunction with startDetection()
     *
     * @throws IllegalStateException if no scenario was created
     */
    public void stopRecord() {
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before stoping any detection!");

        try {
            audioManager.stopRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(activityFP != null)
            activityFP.stopFeatureProcessing();
    }

     /**
     * Saves previously recorded files (sent and received data)
     * @param fileName
     * @throws IOException
     */
    public void saveRecordedFiles(String fileName) throws IOException {
        if(!initialized || audioManager == null)
            throw new IllegalStateException("You must call a create method before calling start/stop or save!");

        if(!audioManager.hasRecordData())
            throw new IllegalStateException("You must record something before saving it");

        try {
            audioManager.saveWaveFiles(fileName);
        }catch(OutOfMemoryError e){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Record too large to be saved!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    private void resetState(){
        activityFP = null;
        gestureFP = null;
    }

    private FeatureProcessor initializeFPForCustomScenario(String extractors, GestureCallback gestureCallback, InferredContextCallback inferredContextCallback, boolean usePreCalibration, boolean noisy) {
        int extractorId = Integer.parseInt(extractors);

        if(extractorId==0)
            return new DummyFeatureProcessor();

        else if(extractorId==1 || extractorId == 2 || extractorId==3){
            List<GestureExtractor> gestureExtractors = new Vector<>();

            if(extractorId==1) {
                gestureExtractors.add(new DownUpGE());
                gestureExtractors.add(new SwipeGE());
            }
            else if(extractorId ==2){
                gestureExtractors.add(new DownUpGE());
            }
            else{
                gestureExtractors.add(new SwipeGE());
            }
            gestureFP = new GestureFP(gestureCallback);
            for (GestureExtractor ge : gestureExtractors) {
                if (!usePreCalibration)
                    initializeGEThresholds(ge, noisy);
                gestureFP.registerGestureExtractor(ge);
            }

            return gestureFP;
        }

        else if(extractorId==4){
            activityFP = new ActivityFP();
            activityFP.registerActivityExtractor(new WorkdeskPresenceAE(inferredContextCallback));
            return activityFP;
        }
        else if(extractorId==5){
            activityFP = new ActivityFP();
            activityFP.registerActivityExtractor(new BedFallAE(inferredContextCallback));
            return activityFP;
        }

        return new DummyFeatureProcessor();
    }
}
