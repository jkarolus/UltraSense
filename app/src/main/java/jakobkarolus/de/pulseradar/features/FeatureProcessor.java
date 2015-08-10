package jakobkarolus.de.pulseradar.features;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.view.PulseRadarFragment;

/**
 * callback for FeatureExtractor upon finishing a feature.
 * Holds a stack of current features
 * <br><br>
 * Created by Jakob on 04.08.2015.
 */
public abstract class FeatureProcessor {

    protected DecimalFormat df = new DecimalFormat("####0.0000");
    private List<Feature> features;
    private double currentFeatureTime;
    private FileWriter featWriter;


    public double getCurrentFeatureTime() {
        return currentFeatureTime;
    }

    public void setCurrentFeatureTime(double currentFeatureTime) {
        this.currentFeatureTime = currentFeatureTime;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public FeatureProcessor() {
        this.features = new Vector<>();
        currentFeatureTime = 0.0;
    }

    /**
     * callback for FeatureExtractors
     * @param feature the Feature that was extracted
     */
    public void processFeature(final Feature feature){

        //save the current feature time to clean up the stack if necessary
        setCurrentFeatureTime(feature.getTime());
        saveFeatureToFile(feature);

        processFeatureOnSubclass(feature);

    }

    /**
     * some FP may decide to decouple feature processing from incoming feature rate, e.g. ActivityFP.<br>
     * Upon receiving the call to this method, these FP should stop their processing
     */
    public abstract void stopFeatureProcessing();

    /**
     * hook method for subclass specific behavior
     * @param feature the Feature that was extracted
     */
    public abstract void processFeatureOnSubclass(final Feature feature);


    /**
     * determines the max time features live on the stack
     * @return max threshold of features time vs current time
     */
    public abstract double getTimeThresholdForGC();

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
            //calibWriter = new FileWriter(new File(PulseRadarFragment.fileDir + "calib.txt"), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void cleanUpFeatureStack() {

        if(getFeatures().size() >= 5){
            ListIterator<Feature> iter = getFeatures().listIterator();
            int sizeBefore = getFeatures().size();
            while(iter.hasNext()){
                Feature f = iter.next();
                if((getCurrentFeatureTime() - f.getTime()) >= getTimeThresholdForGC()) {
                    iter.remove();
                }
            }
            Log.d("FEATURE_GC", "Removed " + (sizeBefore - getFeatures().size()) + " features from the stack");
        }
    }

    protected String printFeatureStack() {
        StringBuffer buffer = new StringBuffer();
        for(Feature f : getFeatures()){
            if(f.getWeight() >= 0.0)
                buffer.append("H");
            else
                buffer.append("L");
        }
        return buffer.toString();
    }

    protected void printFeaturesOnLog() {
        Log.i("FEATURE_STACK", "------------------------------------------");
        for(Feature feature : getFeatures())
            Log.i("FEATURE", "" + df.format(feature.getTime()) + ";" + df.format(feature.getLength()) + ";" + df.format(feature.getWeight()));
        Log.i("FEATURE_STACK_END", "--------------------------------------");
    }
}
