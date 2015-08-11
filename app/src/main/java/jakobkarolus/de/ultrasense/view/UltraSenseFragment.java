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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import jakobkarolus.de.ultrasense.R;
import jakobkarolus.de.ultrasense.UltraSenseModule;
import jakobkarolus.de.ultrasense.algorithm.StftManager;
import jakobkarolus.de.ultrasense.features.activities.InferredContext;
import jakobkarolus.de.ultrasense.features.activities.InferredContextCallback;
import jakobkarolus.de.ultrasense.features.gestures.CalibrationState;
import jakobkarolus.de.ultrasense.features.gestures.Gesture;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;
import jakobkarolus.de.ultrasense.features.gestures.GestureExtractor;

/**
 * Created by Jakob on 25.05.2015.
 */
public class UltraSenseFragment extends Fragment implements GestureCallback, InferredContextCallback {

    private static final String DISPLAY_LAST_SPEC = "DISPLAY_LAST_SPEC";
    private Bitmap lastSpectrogram;
    public static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "PulseRadar" + File.separator;
    public static final double SAMPLE_RATE = 44100.0;

    private StftManager stftManager;

    private Button recordButton;
    private Button gestureDetectionButton;
    private Button activityDetectionButton;

    private Button computeStftButton;
    private Button showLastSpec;
    private Button calibrateButton;
    private TextView countDownView;
    private View calibVisualFeedbackView;
    private View rootView;
    private TextView debugInfo;

    UltraSenseModule ultraSenseModule;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ultraSenseModule = new UltraSenseModule(getActivity());
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
        calibrateButton = (Button) rootView.findViewById(R.id.button_calibrate);
        countDownView = (TextView) rootView.findViewById(R.id.text_countdown);
        calibVisualFeedbackView = (View) rootView.findViewById(R.id.view_calib_recognized);
        debugInfo = (TextView) rootView.findViewById(R.id.text_debug_info);
        activityDetectionButton = (Button) rootView.findViewById(R.id.button_start_activity_detection);

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
                startGestureDetection();
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


    /*
     * Gesture detection
     */
    private void startGestureDetection() {

        //show a dialog for letting the user decide the env noise
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Environment");
        builder.setItems(new String[]{"Silent", "Noisy"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                boolean usePrecalibrated = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsFragment.KEY_USE_PRECALIBRATION, true);
                ultraSenseModule.createGestureDetector(UltraSenseFragment.this, (index == 1), usePrecalibrated);

                //save it for comparison -> the features
                if (ultraSenseModule.getGestureFP() != null)
                    ultraSenseModule.getGestureFP().startFeatureWriter();

                changeDetectionButton(true);
                ultraSenseModule.startDetection();
                updateDebugInfo();
            }
        });
        builder.show();

    }

    private void stopGestureDetection() {
        //save it for comparison -> the features
        if(ultraSenseModule.getGestureFP() != null) {
            ultraSenseModule.getGestureFP().closeFeatureWriter();
        }

        changeDetectionButton(false);
        ultraSenseModule.stopDetection();
    }

    private void changeDetectionButton(boolean isDetecting){

        if(isDetecting){
            gestureDetectionButton.setText(R.string.button_stop_detection);
            gestureDetectionButton.setBackgroundColor(Color.RED);
            gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopGestureDetection();
                }
            });
        }
        else{
            gestureDetectionButton.setBackgroundResource(android.R.drawable.btn_default);
            gestureDetectionButton.setText(R.string.button_start_detection);
            gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startGestureDetection();
                }
            });
        }
    }

    /*
     * Activity detection
     */
    private void startActivityDetection() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose ActivityExtractor");
        builder.setItems(new String[]{"WordkDesk", "Bed"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                if (index == 0)
                    ultraSenseModule.createWorkdeskPresenceDetector(UltraSenseFragment.this);
                else
                    ultraSenseModule.createBedFallDetector(UltraSenseFragment.this);

                //save it for comparison -> the features
                if (ultraSenseModule.getActivityFP() != null)
                    ultraSenseModule.getActivityFP().startFeatureWriter();

                activityDetectionButton.setText(R.string.button_stop_activity_detection);
                activityDetectionButton.setBackgroundColor(Color.RED);
                activityDetectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopActivityDetection();
                    }
                });
                ultraSenseModule.startDetection();
                updateDebugInfo();
            }
        });
        builder.show();

    }

    private void stopActivityDetection() {
        //save it for comparison -> the features
        if(ultraSenseModule.getActivityFP() != null) {
            ultraSenseModule.getActivityFP().closeFeatureWriter();
        }

        activityDetectionButton.setText(R.string.button_start_activity_detection);
        activityDetectionButton.setBackgroundResource(android.R.drawable.btn_default);
        activityDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityDetection();
            }
        });
        ultraSenseModule.stopDetection();

    }


    /*
     * Calibration
     */
    private void startCalibration() {

        //show a dialog for letting the user decide the env noise
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Environment");
        builder.setItems(new String[]{"Silent", "Noisy"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                final boolean noisy = (index == 1);
                ultraSenseModule.createGestureDetector(UltraSenseFragment.this, noisy, false);
                //ask for the GE
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Choose Gesture to calibrate");
                builder.setItems(ultraSenseModule.getGestureFP().getGestureExtractorNames(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index) {
                        //save it for comparison -> the features
                        if (ultraSenseModule.getGestureFP() != null)
                            ultraSenseModule.getGestureFP().startFeatureWriter();
                        ultraSenseModule.getGestureFP().startCalibrating(ultraSenseModule.getGestureFP().getGestureExtractors().get(index), noisy);
                        displayCountdownAndStartCalibrationRun();
                    }
                });
                builder.show();

            }
        });
        builder.show();
    }

    private void displayCountdownAndStartCalibrationRun() {

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
                ultraSenseModule.startDetection();
            }
        }.start();

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
                    ultraSenseModule.stopDetection();

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
                                    displayCountdownAndStartCalibrationRun();
                                }
                            });
                            timer.cancel();
                        }
                    }, 200, 100);
                }
            }
        });
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
                if (ultraSenseModule.getGestureFP() != null) {
                    ultraSenseModule.getGestureFP().closeFeatureWriter();
                }

                changeDetectionButton(false);
                ultraSenseModule.stopDetection();

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


    /*
     * callback implementations
     */

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

        Log.i("CONTEXT", "Changed from " + oldContext + " to " + newContext + ": " + reason);

    }


    /*
     * Recording to file
     */

    private void startRecord() throws IOException {

        ultraSenseModule.createCustomScenario(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        updateDebugInfo();

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
        ultraSenseModule.startRecord();
    }


    private void stopRecord() throws IOException {

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
        ultraSenseModule.stopRecord();


        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AskForFileNameDialog fileNameDialog = new AskForFileNameDialog();
        fileNameDialog.show(ft, "FileNameDialog");
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
        if(ultraSenseModule.getAudioManager().hasRecordData()) {
            new ComputeSTFTTask().execute();
        }
        else{
            Toast.makeText(getActivity(), "No latest record available", Toast.LENGTH_LONG).show();
        }
    }




    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(true);
        menu.findItem(R.id.action_compute_stft).setVisible(true);
        menu.findItem(R.id.action_show_last).setVisible(true);
        menu.findItem(R.id.action_update_debug_info).setVisible(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

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



    private void updateDebugInfo() {
        StringBuffer buffer = new StringBuffer();
        if(ultraSenseModule != null){
            buffer.append(ultraSenseModule.printFeatureDetectionParameters() + "\n\n");
        }

        if(ultraSenseModule.getGestureFP() != null) {
            for (GestureExtractor ge : ultraSenseModule.getGestureFP().getGestureExtractors()) {
                buffer.append(ge.getName() + ": " + ge.getThresholds() + "\n");
            }
        }
        debugInfo.setText(buffer.toString());
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
            double[] data = ultraSenseModule.getAudioManager().getRecordData(true);
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
            fileName.setText("");
            builder.setView(view);
            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                ultraSenseModule.saveRecordedFiles(fileName.getText().toString());
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