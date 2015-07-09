package jakobkarolus.de.pulseradar.features;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
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

    private List<Feature> features;
    private List<GestureExtractor> gestureExtractors;
    private Context ctx;
    private FileWriter featWriter;

    public FeatureProcessor(Context ctx){
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


    public void processFeature(final Feature feature){

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

        //process feature
        features.add(feature);
        for(GestureExtractor ge : gestureExtractors) {
            final List<Gesture> gestures = ge.detectGesture(features);
            for(Gesture g : gestures) {
                Log.e(TAG, g.toString());
            }
        }
    }
}
