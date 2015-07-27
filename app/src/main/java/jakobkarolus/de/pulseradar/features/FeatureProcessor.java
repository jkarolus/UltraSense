package jakobkarolus.de.pulseradar.features;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private List<Feature> features;
    private List<GestureExtractor> gestureExtractors;
    private Activity ctx;
    private FileWriter featWriter;
    private double currentFeatureTime;

    public FeatureProcessor(Activity ctx){
        this.ctx = ctx;
        features = new Vector<>();
        gestureExtractors = new Vector<>();
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

    public void closeFeatureWriter(){
        try {
            featWriter.flush();
            featWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startFeatureWriter(){
        try {
            featWriter = new FileWriter(new File(PulseRadarFragment.fileDir + "feat.txt"), false);
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


    public void processFeature(final Feature feature){

        currentFeatureTime = feature.getTime();
        saveFeatureToFile(feature);

        //process feature
        features.add(feature);
        Log.i("FEATURE", "" + feature.getTime() + "," + feature.getLength() + "," + feature.getWeight());
        for(GestureExtractor ge : gestureExtractors) {
            final List<Gesture> gestures = ge.detectGesture(features);
            for(final Gesture g : gestures) {
                Log.i("GESTURE", g.toString());
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ctx, g.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        cleanUpFeatureStack();
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
        return ctx;
    }
}
