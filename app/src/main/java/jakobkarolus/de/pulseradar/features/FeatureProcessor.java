package jakobkarolus.de.pulseradar.features;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.gestures.Gesture;
import jakobkarolus.de.pulseradar.features.gestures.GestureExtractor;
import jakobkarolus.de.pulseradar.view.PulseRadarFragment;

/**
 * Combines Features and their respective Gesture, by keeping a List of current Feature and delegating
 * Gesture recognition to its GestureExtractors
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class FeatureProcessor {

    private static final String TAG = "GESTURE";
    private static final double TIME_THRESHOLD = 5.0;
    private static final int MAX_CALIBRATION_RUNS = 5;

    private List<Feature> features;
    private List<GestureExtractor> gestureExtractors;
    private PulseRadarFragment ctx;
    private FileWriter featWriter;
    private double currentFeatureTime;
    private int calibrationRuns;
    private boolean isCalibrating;
    private GestureExtractor calibrator;
    private FileWriter calibWriter;
    private DecimalFormat df = new DecimalFormat("0.0000E0");

    public FeatureProcessor(PulseRadarFragment ctx){
        this.ctx = ctx;
        features = new Vector<>();
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

    public void closeFeatureWriter(){
        try {
            featWriter.flush();
            featWriter.close();
            calibWriter.flush();
            calibWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startFeatureWriter(){
        try {
            featWriter = new FileWriter(new File(PulseRadarFragment.fileDir + "feat.txt"), false);
            calibWriter = new FileWriter(new File(PulseRadarFragment.fileDir + "calib.txt"), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveFeatureToFile(final Feature feature){
        //save feature
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    featWriter.write(feature.getTime() + "," + feature.getLength() + "," + feature.getWeight() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    public void startCalibrating(GestureExtractor ge){
        this.calibrator = ge;
        this.calibrator.resetThresholds();
        calibrationRuns = 0;
        isCalibrating = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    calibWriter.write(calibrator.getThresholds());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        //show a CD dialog
    }

    public void processFeature(final Feature feature){

        currentFeatureTime = feature.getTime();
        saveFeatureToFile(feature);
        Log.i("FEATURE", "" + df.format(feature.getTime()) + ";" + df.format(feature.getLength()) + ";" + df.format(feature.getWeight()));
        features.add(feature);
        Log.i("FEATURE_STACK", printFeatureStack());

        if(isCalibrating && calibrationRuns < MAX_CALIBRATION_RUNS && calibrator != null){
            boolean calibrated = calibrator.calibrate(features);
            final String text;
            if(calibrated)
                text = "Successfully used for calibration";
            else
                text = "Feature does not match Gesture characteristics";
            if(!calibrated)
                features.clear();
            if(calibrated) {
                calibrationRuns++;
            }
            if(calibrationRuns < MAX_CALIBRATION_RUNS)
                ctx.onCalibrationStep(calibrated);


            ctx.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx.getActivity(), text, Toast.LENGTH_SHORT).show();
                }
            });

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        calibWriter.write(calibrator.getThresholds());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }

        if(isCalibrating && calibrationRuns >= MAX_CALIBRATION_RUNS){
            ctx.onCalibrationFinished(calibrator.getThresholds(), calibrator.getName());
            isCalibrating = false;
            //save data internally to access during later detection
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(ctx.getActivity().openFileOutput(calibrator.getName() + ".calib", Context.MODE_PRIVATE));
                        out.writeObject(calibrator.getThresholdMap());
                        out.flush();
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        else {
            for (GestureExtractor ge : gestureExtractors) {
                final List<Gesture> gestures = ge.detectGesture(features);
                for (final Gesture g : gestures) {
                    Log.i("GESTURE", g.toString());
                    ctx.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctx.getActivity(), g.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            cleanUpFeatureStack();
        }

    }

    private String printFeatureStack() {
        StringBuffer buffer = new StringBuffer();
        for(Feature f : features){
            if(f.getWeight() >= 0.0)
                buffer.append("H ");
            else
                buffer.append("L ");
        }
        return buffer.toString();
    }

    private void cleanUpFeatureStack() {

        if(features.size() >= 5){
            ListIterator<Feature> iter = features.listIterator();
            int sizeBefore = features.size();
            while(iter.hasNext()){
                Feature f = iter.next();
                if((currentFeatureTime - f.getTime()) >= TIME_THRESHOLD) {
                    iter.remove();
                }
            }
            Log.i("GESTURE_GC", "Removed " + (sizeBefore - features.size()) + " features from the stack");
        }
    }

    public Activity getActivity(){
        return ctx.getActivity();
    }
}
