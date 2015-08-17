package jakobkarolus.de.ultrasense.features;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.ultrasense.view.UltraSenseFragment;

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


    /**
     *
     * @return the current feature time, in other words the time at with the last feature arrived
     */
    public double getCurrentFeatureTime() {
        return currentFeatureTime;
    }

    public void setCurrentFeatureTime(double currentFeatureTime) {
        this.currentFeatureTime = currentFeatureTime;
    }

    /**
     *
     * @return current list of feature
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * creates a new FeatureProcessor at time 0.0
     */
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

    /**
     * saves the given feature to a file.<br>
     * If used you must have called startFeatureWriter() before and closeFeatureWriter() when recording/detection is finished
     * @param feature the feature to save
     */
    public void saveFeatureToFile(final Feature feature){
        if(featWriter != null) {
            //save feature
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        featWriter.write(feature.getTime() + "," + feature.getLength() + "," + feature.getWeight() + "\n");
                        featWriter.flush();
                    } catch (IOException e) {
                        //feature detection was already closed when this feature came in
                    }
                }
            });
            thread.start();
        }
    }

    public void closeFeatureWriter(){
        if(featWriter != null) {
            try {
                featWriter.flush();
                featWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startFeatureWriter(){
        try {
            featWriter = new FileWriter(new File(UltraSenseFragment.fileDir + "feat.txt"), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * cleans up the feature stack depending on the GC_threshold
     */
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

    /**
     *
     * @return String representation of the current feature stack (list of high and low features)
     */
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

    /**
     * prints detailed information of the feature stack to the logcat
     */
    protected void printFeaturesOnLog() {
        Log.d("FEATURE_STACK", "------------------------------------------");
        for(Feature feature : getFeatures())
            Log.d("FEATURE", "" + df.format(feature.getTime()) + ";" + df.format(feature.getLength()) + ";" + df.format(feature.getWeight()));
        Log.d("FEATURE_STACK_END", "--------------------------------------");
    }
}
