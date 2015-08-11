package jakobkarolus.de.ultrasense.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import jakobkarolus.de.ultrasense.R;
import jakobkarolus.de.ultrasense.algorithm.AlgoHelper;
import jakobkarolus.de.ultrasense.algorithm.StftManager;
import jakobkarolus.de.ultrasense.audio.AudioManager;
import jakobkarolus.de.ultrasense.audio.CWSignalGenerator;
import jakobkarolus.de.ultrasense.audio.FMCWSignalGenerator;
import jakobkarolus.de.ultrasense.audio.SignalGenerator;
import jakobkarolus.de.ultrasense.features.DummyFeatureDetector;
import jakobkarolus.de.ultrasense.features.FeatureDetector;
import jakobkarolus.de.ultrasense.features.GaussianFE;
import jakobkarolus.de.ultrasense.features.MeanBasedFD;
import jakobkarolus.de.ultrasense.features.TestDataGestureFP;
import jakobkarolus.de.ultrasense.features.activities.ActivityFP;
import jakobkarolus.de.ultrasense.features.activities.BedFallAE;
import jakobkarolus.de.ultrasense.features.activities.InferredContext;
import jakobkarolus.de.ultrasense.features.activities.InferredContextCallback;
import jakobkarolus.de.ultrasense.features.activities.WorkdeskPresenceAE;
import jakobkarolus.de.ultrasense.features.gestures.CalibrationState;
import jakobkarolus.de.ultrasense.features.gestures.DownUpGE;
import jakobkarolus.de.ultrasense.features.gestures.Gesture;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;
import jakobkarolus.de.ultrasense.features.gestures.GestureExtractor;
import jakobkarolus.de.ultrasense.features.gestures.GestureFP;
import jakobkarolus.de.ultrasense.features.gestures.SwipeGE;

/**
 * Created by Jakob on 25.05.2015.
 */
public class UltraSenseFragment extends Fragment implements GestureCallback, InferredContextCallback {

    private static final String DISPLAY_LAST_SPEC = "DISPLAY_LAST_SPEC";
    private Bitmap lastSpectrogram;
    public static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "PulseRadar" + File.separator;
    public static final double SAMPLE_RATE = 44100.0;

    private AudioManager audioManager;
    private StftManager stftManager;
    private FeatureDetector featureDetector;

    private Button recordButton;
    private Button gestureDetectionButton;
    private Button activityDetectionButton;

    private Button computeStftButton;
    private Button showLastSpec;
    private Button calibrateButton;
    private Button testDetection;
    private TextView countDownView;
    private View calibVisualFeedbackView;
    private View rootView;
    private TextView debugInfo;
    private boolean usePreCalibration=true;

    private GestureFP gestureFP;
    private ActivityFP activityFP;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = new AudioManager(getActivity());
        stftManager = new StftManager();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pulse_radar, container, false);
        recordButton = (Button) rootView.findViewById(R.id.button_start_record);
        gestureDetectionButton = (Button) rootView.findViewById(R.id.button_start_detection);
        computeStftButton = (Button) rootView.findViewById(R.id.button_fft);
        showLastSpec = (Button) rootView.findViewById(R.id.button_last_spec);
        testDetection = (Button) rootView.findViewById(R.id.button_test_detection);
        calibrateButton = (Button) rootView.findViewById(R.id.button_calibrate);
        countDownView = (TextView) rootView.findViewById(R.id.text_countdown);
        calibVisualFeedbackView = (View) rootView.findViewById(R.id.view_calib_recognized);
        debugInfo = (TextView) rootView.findViewById(R.id.text_debug_info);
        activityDetectionButton = (Button) rootView.findViewById(R.id.button_start_activity_detection);
        updateDebugInfo();

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDetection();
            }
        });
        computeStftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                computeStft();
            }
        });
        showLastSpec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLastSpec();
            }
        });
        testDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    testDetection();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCalibration();
            }
        });
        activityDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityDetection();
            }
        });

        return rootView;
    }

    private void startActivityDetection() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose ActivityExtractor");
        builder.setItems(new String[]{"WordkDesk", "Bed"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                setUpSignalAndFeatureStuff(false, false, true, index, false);

                //save it for comparison -> the features
                if (activityFP != null)
                    activityFP.startFeatureWriter();

                activityDetectionButton.setText(R.string.button_stop_activity_detection);
                activityDetectionButton.setBackgroundColor(Color.RED);
                activityDetectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopActivityDetection();
                    }
                });
                audioManager.startDetection();
            }
        });
        builder.show();

    }

    private void stopActivityDetection() {
        //save it for comparison -> the features
        if(activityFP != null) {
            activityFP.closeFeatureWriter();
        }

        activityDetectionButton.setText(R.string.button_start_activity_detection);
        activityDetectionButton.setBackgroundResource(android.R.drawable.btn_default);
        activityDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityDetection();
            }
        });
        audioManager.stopDetection();
        activityFP.stopFeatureProcessing();

    }

    private void startCalibration() {

        setUpSignalAndFeatureStuff(false, true, false, 0, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Gesture to calibrate (only silent env)");
        builder.setItems(gestureFP.getGestureExtractorNames(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {


                //save it for comparison -> the features
                if (gestureFP != null)
                    gestureFP.startFeatureWriter();
                gestureFP.startCalibrating(gestureFP.getGestureExtractors().get(index));
                displayCountdownAndStartDetection();
            }
        });
        builder.show();


    }

    private void displayCountdownAndStartDetection() {

        countDownView.setText("3");
        countDownView.setVisibility(View.VISIBLE);
        CountDownTimer timer = new CountDownTimer(3000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                int time = (int) Math.ceil(((double) millisUntilFinished / 1000.0));
                countDownView.setText(""+time);
            }

            @Override
            public void onFinish() {
                countDownView.setVisibility(View.INVISIBLE);
                changeDetectionButton(true);
                audioManager.startDetection();
            }
        }.start();

    }

    private void changeDetectionButton(boolean isDetecting){

        if(isDetecting){
            gestureDetectionButton.setText(R.string.button_stop_detection);
            gestureDetectionButton.setBackgroundColor(Color.RED);
            gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopDetection();
                }
            });
        }
        else{
            gestureDetectionButton.setBackgroundResource(android.R.drawable.btn_default);
            gestureDetectionButton.setText(R.string.button_start_detection);
            gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startDetection();
                }
            });
        }
    }

    @Override
    public void onCalibrationStep(final CalibrationState calibState){

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDebugInfo();

                //only react on completed (successful or failed) calibration
                if (calibState == CalibrationState.SUCCESSFUL || calibState == CalibrationState.FAILED) {
                    changeDetectionButton(false);
                    audioManager.stopDetection();

                    if (calibState == CalibrationState.SUCCESSFUL) {
                        calibVisualFeedbackView.setBackgroundColor(Color.GREEN);
                        calibVisualFeedbackView.setVisibility(View.VISIBLE);
                    }
                    if (calibState == CalibrationState.FAILED) {
                        calibVisualFeedbackView.setBackgroundColor(Color.RED);
                        calibVisualFeedbackView.setVisibility(View.VISIBLE);
                    }

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    calibVisualFeedbackView.setVisibility(View.INVISIBLE);
                                    displayCountdownAndStartDetection();
                                }
                            });
                            timer.cancel();
                        }
                    }, 200, 100);
                }
            }
        });
    }

    private void stopDetection() {
        //save it for comparison -> the features
        if(gestureFP != null) {
            gestureFP.closeFeatureWriter();
        }

        changeDetectionButton(false);
        audioManager.stopDetection();
    }

    private void startDetection() {

        //show a dialog for letting the user decide the env noise
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Environment");
        builder.setItems(new String[]{"Silent", "Noisy"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {


                setUpSignalAndFeatureStuff(false, false, false, 0, (index == 1));

                //save it for comparison -> the features
                if (gestureFP != null)
                    gestureFP.startFeatureWriter();

                changeDetectionButton(true);
                audioManager.startDetection();
            }
        });
        builder.show();

    }

    private void testDetection() throws IOException {

        setUpSignalAndFeatureStuff(true, false, false, 0, false);

        new TestDetectionTask().execute();


    }

    @Override
    public void onCalibrationFinished(final Map<String, Double> thresholds, final String prettyPrintThresholds, final String name) {


        //save data internally to access during later detection
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(getActivity().openFileOutput(name + ".calib", Context.MODE_PRIVATE));
                    out.writeObject(thresholds);
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gestureFP != null) {
                    gestureFP.closeFeatureWriter();
                }

                changeDetectionButton(false);
                audioManager.stopDetection();

                //setUpSignalAndFeatureStuff(false, false, false, 0, false);
                updateDebugInfo();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Thresholds " + name);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                    }
                });
                builder.setMessage(prettyPrintThresholds);
                builder.show();
            }
        });
    }

    @Override
    public void onGestureDetected(final Gesture gesture) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), gesture.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onInferredContextChange(InferredContext oldContext, InferredContext newContext, String reason) {

        Log.i("CONTEXT", "Changed from " + oldContext + " to " + newContext +": " + reason);

    }

    private class TestDetectionTask extends AsyncTask<Void, String, Void> {

        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(getActivity(), "Testing Detection", "Please wait", true, false);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            pd.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pd.dismiss();
            gestureFP.closeFeatureWriter();

        }


        @Override
        protected Void doInBackground(Void... params) {

            try {

                //save it for comparison -> the features
                gestureFP.startFeatureWriter();

                List<Double> dataList = new Vector<>();
                DataInputStream din = new DataInputStream(new FileInputStream(fileDir + "testing/up_down_s3.bin"));
                boolean notEnded = true;

                publishProgress(new String[]{"Reading values"});
                while (notEnded) {
                    try {
                        dataList.add(din.readDouble());;
                    } catch (EOFException e) {
                        notEnded = false;
                    }
                }
                double[] data = new double[dataList.size()];
                for (int i = 0; i < data.length; i++)
                    data[i] = dataList.get(i);

                publishProgress(new String[]{"Calculating features"});
                int length = 4 * 4096;
                double[] buffer = new double[length];
                for (int i = 0; i <= data.length - length; i += length) {
                    System.arraycopy(data, i, buffer, 0, length);
                    featureDetector.checkForFeatures(buffer, false);
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }
    }

    private void showLastSpec() {
        if(lastSpectrogram != null) {
            Bundle args = new Bundle();
            args.putBoolean(DISPLAY_LAST_SPEC, true);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment spec = new Spectrogram();
            spec.setArguments(args);
            ft.replace(R.id.container, spec, Spectrogram.class.getName());
            ft.addToBackStack(Spectrogram.class.getName());
            ft.commit();
        }
        else
            Toast.makeText(getActivity(), "No latest spectrogram available", Toast.LENGTH_LONG).show();
    }


    private void computeStft() {
        if(audioManager.hasRecordData()) {
            new ComputeSTFTTask().execute();
        }
        else{
            Toast.makeText(getActivity(), "No latest record available", Toast.LENGTH_LONG).show();
        }
    }

    private void startRecord() throws IOException {

        setUpSignalAndFeatureStuff(false, false, false, 0, false);

        //save it for comparison -> the features
        if(gestureFP != null)
            gestureFP.startFeatureWriter();

        recordButton.setText(R.string.button_stop_record);
        recordButton.setBackgroundColor(Color.RED);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    stopRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        audioManager.startRecord();
    }


    private void stopRecord() throws IOException {

        //save it for comparison -> the features
        if(gestureFP != null) {
            gestureFP.closeFeatureWriter();
        }

        recordButton.setText(R.string.button_start_record);
        recordButton.setBackgroundResource(android.R.drawable.btn_default);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        computeStftButton.setEnabled(true);
        audioManager.stopRecord();


        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AskForFileNameDialog fileNameDialog = new AskForFileNameDialog();
        fileNameDialog.show(ft, "FileNameDialog");
    }

    private void setUpSignalAndFeatureStuff(boolean testData, boolean isCalibrating, boolean activityDetection, int activityIndex, boolean noisy){
        SignalGenerator signalGen = getSignalGeneratorForMode(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        audioManager.setSignalGenerator(signalGen);
        initializeFeatureDetector(testData, isCalibrating, activityDetection, activityIndex, noisy);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(true);
        menu.findItem(R.id.action_compute_stft).setVisible(true);
        menu.findItem(R.id.action_show_last).setVisible(true);
        menu.findItem(R.id.action_test_detection).setVisible(true);
        menu.findItem(R.id.action_update_debug_info).setVisible(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_test_detection) {
            try {
                testDetection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        if (id == R.id.action_compute_stft) {
            computeStft();
            return true;
        }

        if (id == R.id.action_show_last){
            showLastSpec();
            return true;
        }

        if(id == R.id.action_update_debug_info) {
            updateDebugInfo();
            return true;
        }
        return false;
    }


    private void initializeFeatureDetector(boolean testData, boolean isCalibrating, boolean activityDetection, int activityIndex, boolean noisy) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String mode = sharedPreferences.getString(SettingsFragment.PREF_MODE, "CW");
        try{
            if(mode.equals(SettingsFragment.CW_MODE)){
                int fftLength = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_FFT_LENGTH, ""));
                double hopSizeFraction = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_HOPSIZE, ""));
                int hopSize = (int) (hopSizeFraction * fftLength);
                int halfCarrierWidth = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_HALF_CARRIER_WIDTH, ""));
                double dbThreshold = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_DB_THRESHOLD, ""));
                double highFeatureThr = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_HIGH_FEAT_THRESHOLD, ""));
                double lowFeatureThr = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_LOW_FEAT_THRESHOLD, ""));
                int slackWidth = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_FEAT_SLACK, ""));
                double freq = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_CW_FREQ, ""));
                usePreCalibration = sharedPreferences.getBoolean(SettingsFragment.KEY_USE_PRECALIBRATION, true);

                if(!activityDetection) {
                    if(!noisy)
                        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, freq, halfCarrierWidth, dbThreshold, highFeatureThr, lowFeatureThr, slackWidth, AlgoHelper.getHannWindow(fftLength), false, 0.0);
                    else
                        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, freq, 3, -50.0, 3, 2, 1, AlgoHelper.getHannWindow(fftLength), true, 20);

                }
                else{
                    //use predefined values for activity recognition
                    if(activityIndex == 0)
                        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, freq, 5, -60.0, 1.0, 0.5, 10, AlgoHelper.getHannWindow(fftLength), true, 8.0);
                    else
                        featureDetector = new MeanBasedFD(SAMPLE_RATE, fftLength, hopSize, freq, 5, -60.0, 2.0, 1.0, 5, AlgoHelper.getHannWindow(fftLength), true, 20.0);
                }

            }
            else
                featureDetector = new DummyFeatureDetector(0.0);

        }catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Specified Parameters are not valid!", Toast.LENGTH_LONG).show();
            featureDetector = new DummyFeatureDetector(0.0);
        }

        if(testData)
            gestureFP = new TestDataGestureFP(this, getActivity());
        else
            gestureFP = new GestureFP(this);

        if(!activityDetection) {
            //TODO: GesturesExtractors as preferences?
            List<GestureExtractor> gestureExtractors = new Vector<>();
            //gestureExtractors.add(new DownGE());
            //gestureExtractors.add(new UpGE());
            gestureExtractors.add(new DownUpGE());
            gestureExtractors.add(new SwipeGE());

            for (GestureExtractor ge : gestureExtractors) {
                if (!usePreCalibration)
                    initializeGEThresholds(ge);
                gestureFP.registerGestureExtractor(ge);
            }
            featureDetector.registerFeatureExtractor(new GaussianFE(gestureFP));

        }
        else{
            activityFP = new ActivityFP();
            if(activityIndex==0)
                activityFP.registerActivityExtractor(new WorkdeskPresenceAE(this));
            else
                activityFP.registerActivityExtractor(new BedFallAE(this));

            featureDetector.registerFeatureExtractor(new GaussianFE(activityFP));
        }

        updateDebugInfo();
        audioManager.setFeatureDetector(featureDetector);


    }

    private void updateDebugInfo() {
        StringBuffer buffer = new StringBuffer();

        if(featureDetector != null)
            buffer.append(featureDetector.printParameters() + "\n\n");

        if(gestureFP != null) {
            for (GestureExtractor ge : gestureFP.getGestureExtractors()) {
                buffer.append(ge.getName() + ": " + ge.getThresholds() + "\n");
            }
        }

        if(buffer.length() == 0)
            debugInfo.setText("No Info on GEs available!\n");
        else
            debugInfo.setText(buffer.toString());
    }

    private boolean initializeGEThresholds(GestureExtractor ge) {
        try {
            ObjectInputStream in = new ObjectInputStream(getActivity().openFileInput(ge.getName() + ".calib"));
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


    private SignalGenerator getSignalGeneratorForMode(SharedPreferences sharedPreferences) {
        String mode = sharedPreferences.getString(SettingsFragment.PREF_MODE, "CW");
        try {
            if(mode.equals(SettingsFragment.FMCW_MODE)) {
                double botFreq = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_FMCW_BOT_FREQ, ""));
                double topFreq = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_FMCW_TOP_FREQ, ""));
                double chirpDur = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_FMCW_CHIRP_DUR, ""));
                double chirpCycles = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_FMCW_CHIRP_CYCLES, ""));
                boolean rampUp = sharedPreferences.getBoolean(SettingsFragment.KEY_FMCW_RAMP_UP, false);

                return new FMCWSignalGenerator(topFreq, botFreq, chirpDur, chirpCycles, 44100, 1.0f, !rampUp);
            }
            else {
                double freq = Double.parseDouble(sharedPreferences.getString(SettingsFragment.KEY_CW_FREQ, ""));
                return new CWSignalGenerator(freq, 0.1, 1.0, 44100);
            }
        }catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Specified Parameters are not valid! Using defaults!", Toast.LENGTH_LONG).show();
            return new CWSignalGenerator(20000, 0.1, 1.0, 44100);
        }
    }


    private class ComputeSTFTTask extends AsyncTask<Void, String, Void> {

        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(getActivity(), "Computing STFT", "Please wait", true, false);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            pd.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pd.dismiss();

            if(stftManager.getCurrentSTFT() != null) {
                Bundle args = new Bundle();
                args.putBoolean(DISPLAY_LAST_SPEC, false);

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment spec = new Spectrogram();
                spec.setArguments(args);
                ft.replace(R.id.container, spec, Spectrogram.class.getName());
                ft.addToBackStack(Spectrogram.class.getName());
                ft.commit();
            }
            else{
                Toast.makeText(getActivity(), "Sequence too short", Toast.LENGTH_LONG).show();
            }
        }


        @Override
        protected Void doInBackground(Void... params) {
            double[] data = audioManager.getRecordData(true);
            if(data.length != 0) {
                stftManager.setData(data);
                publishProgress("Applying high pass filter");
                stftManager.applyHighPassFilterOld();
                publishProgress("Modulating signal");
                stftManager.modulate(19000);
                publishProgress("Downsampling");
                stftManager.downsample(4);
                publishProgress("STFT");
                stftManager.computeSTFT();
            }
            return null;
        }
    }

    private void saveDataToFile(String fileName, double[] data) {
        try {
            FileWriter writer = new FileWriter(new File(fileDir + fileName  + ".txt"), false);
            for(int i=0; i < data.length; i++){
                writer.write(data[i] + " ");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveStftToFile(String fileName, double[][] stft) {

        try {
            FileWriter writer = new FileWriter(new File(fileDir + fileName  + ".txt"), false);
            for(int i=0; i < stft.length; i++){
                for(int j=0; j < stft[i].length; j++){
                    if(j == stft[i].length-1)
                        writer.write(stft[i][j] + ";\n");
                    else
                        writer.write(stft[i][j] + ",");
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("ValidFragment")
    public class AskForFileNameDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_record_name,	null);
            final EditText fileName = (EditText) view.findViewById(R.id.input_filename_record);
            fileName.setText("test");
            builder.setView(view);
            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                audioManager.saveWaveFiles(fileName.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
            builder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AskForFileNameDialog.this.getDialog().cancel();
                        }
                    });

            return builder.create();
        }
    }

    @SuppressLint("ValidFragment")
    public class Spectrogram extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            menu.findItem(R.id.action_settings).setVisible(false);
            menu.findItem(R.id.action_compute_stft).setVisible(false);
            menu.findItem(R.id.action_show_last).setVisible(false);
            menu.findItem(R.id.action_test_detection).setVisible(false);
            menu.findItem(R.id.action_update_debug_info).setVisible(false);

        }

        public Spectrogram(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_spectrogram, container, false);
            TouchImageView spec = (TouchImageView) rootView.findViewById(R.id.spectrogram);

            if(!getArguments().getBoolean(DISPLAY_LAST_SPEC))
                lastSpectrogram = createBitmap(convertToGreyscale(stftManager.getCurrentSTFT()));
            spec.setImageBitmap(lastSpectrogram);

            return rootView;
        }


        private Bitmap createBitmap(int[][] data){
            int height = data[0].length;
            int width = data.length;
            //Toast.makeText(PulseRadar.this, "#frequencies: " + height + ", #timesteps: " + width, Toast.LENGTH_LONG).show();

            int widthFactor = 16;
            int[] arrayCol = new int[width*height*widthFactor/2];
            int counter = 0;
            for(int i = height/2-1; i >= 0; i--) {
                for(int j = 0; j < width; j++) {

                    for(int k=0; k < widthFactor; k++)
                        arrayCol[counter+k] = data[j][i];

                    counter+=widthFactor;
                }
            }
            return Bitmap.createBitmap(arrayCol, width * widthFactor, height / 2, Bitmap.Config.ARGB_8888);
        }
    }

    private int[][] convertToGreyscale(double[][] stft) {
        double[] maxMin = findMaxMin(stft);
        double maxValue = maxMin[0];
        double minValue = maxMin[1];
        int[][] spec = new int[stft.length][stft[0].length];
        for (int i = 0; i < stft.length; i++) {
            for (int j = 0; j < stft[i].length; j++) {
                int value = Math.max(0, Math.min(255, (int) (((stft[i][j] - minValue) / (maxValue - minValue)) * 255.0)));
                value = 255-value;
                spec[i][j] = Color.rgb(value, value, value);
            }
        }
        return spec;
    }

    /**
     *
     * @param stft current STFT
     * @return two entry double array; [0] is max, [1] is min
     */
    private double[] findMaxMin(double[][] stft) {
        double currentMax = Double.MIN_VALUE;
        double currentMin = Double.MAX_VALUE;

        for(int i=0; i < stft.length; i++){
            for(int j=0; j < stft[i].length; j++){
                if(stft[i][j] > currentMax)
                    currentMax = stft[i][j];
                if(stft[i][j] < currentMin)
                    currentMin = stft[i][j];
            }
        }
        double[] maxMin = {currentMax, currentMin};
        return maxMin;
    }
}