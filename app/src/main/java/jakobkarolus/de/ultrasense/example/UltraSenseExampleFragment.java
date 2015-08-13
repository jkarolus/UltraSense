package jakobkarolus.de.ultrasense.example;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import jakobkarolus.de.ultrasense.R;
import jakobkarolus.de.ultrasense.UltraSenseModule;
import jakobkarolus.de.ultrasense.features.activities.InferredContext;
import jakobkarolus.de.ultrasense.features.activities.InferredContextCallback;
import jakobkarolus.de.ultrasense.features.gestures.CalibrationState;
import jakobkarolus.de.ultrasense.features.gestures.Gesture;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;

/**
 * Fragment for the example activity
 * <br><br>
 * Created by Jakob on 10.08.2015.
 */
public class UltraSenseExampleFragment extends Fragment implements GestureCallback, InferredContextCallback {

    private Button gestureDetectionButton;
    private Button activityDetectionButton;
    private TextView tvLogcat;
    private UltraSenseModule ultraSenseModule;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.ultraSenseModule = new UltraSenseModule(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ultrasense_example, container, false);
        gestureDetectionButton = (Button) rootView.findViewById(R.id.button_start_detection);
        activityDetectionButton = (Button) rootView.findViewById(R.id.button_start_activity_detection);
        tvLogcat = (TextView) rootView.findViewById(R.id.tv_logcat);
        tvLogcat.setMovementMethod(new ScrollingMovementMethod());


        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateLog();
            }
        }).start();
        */


        gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGestureDetection();
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogcat.setText("");
            }
        });

        //show a dialog for the two scenarios
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Activity");
        builder.setItems(new String[]{"WorkDesk", "BedFall"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                if (index == 0)
                    ultraSenseModule.createWorkdeskPresenceDetector(UltraSenseExampleFragment.this);
                else
                    ultraSenseModule.createBedFallDetector(UltraSenseExampleFragment.this);

                activityDetectionButton.setText(R.string.button_stop_activity_detection);
                activityDetectionButton.setBackgroundColor(Color.RED);
                activityDetectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopActivityDetection();
                    }
                });
                ultraSenseModule.startDetection();
            }
        });
        builder.show();

    }

    private void stopActivityDetection() {
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

    private void startGestureDetection() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogcat.setText("");
            }
        });


        //show a dialog for letting the user decide the env noise
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Environment");
        builder.setItems(new String[]{"Silent", "Noisy"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {

                ultraSenseModule.createGestureDetector(UltraSenseExampleFragment.this, (index==1), true);

                gestureDetectionButton.setText(R.string.button_stop_detection);
                gestureDetectionButton.setBackgroundColor(Color.RED);
                gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopGestureDetection();
                    }
                });
                ultraSenseModule.startDetection();
            }
        });
        builder.show();
    }

    private void stopGestureDetection() {
        gestureDetectionButton.setText(R.string.button_start_detection);
        gestureDetectionButton.setBackgroundResource(android.R.drawable.btn_default);
        gestureDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGestureDetection();
            }
        });
        ultraSenseModule.stopDetection();
    }

    private void updateLog() {
        try {
            Process process = Runtime.getRuntime().exec("logcat");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            final StringBuilder log=new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvLogcat.setText(log.toString());
                    }
                });
            }
        }
        catch (IOException e) {}
    }

    @Override
    public void onCalibrationStep(CalibrationState calibState) {

        //no calibration in this example
    }

    @Override
    public void onCalibrationFinished(Map<String, Double> thresholds, String prettyPrintThresholds, String name) {
        //no calibration in this example

    }

    @Override
    public void onGestureDetected(final Gesture gesture) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogcat.setText(tvLogcat.getText() + "\n" + gesture.toString());
            }
        });
    }

    @Override
    public void onInferredContextChange(final InferredContext oldContext, final InferredContext newContext, final String reason) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogcat.setText(tvLogcat.getText() + "\n" + "Changed from " + oldContext + " to " + newContext + ": " + reason);

            }
        });
        Log.i("CONTEXT", "Changed from " + oldContext + " to " + newContext + ": " + reason);

    }
}
