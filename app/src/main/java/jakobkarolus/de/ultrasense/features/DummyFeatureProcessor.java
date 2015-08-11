package jakobkarolus.de.ultrasense.features;

import android.util.Log;

import java.util.Collections;
import java.util.Comparator;

/**
 * Dummy FP for recording session.<br>
 * Saves all features to a file, logs them and cleans up the stack but does not process any of them
 * <br><br>
 * Created by Jakob on 11.08.2015.
 */
public class DummyFeatureProcessor extends FeatureProcessor{
    @Override
    public void stopFeatureProcessing() {
        //nothing to do here
    }

    @Override
    public void processFeatureOnSubclass(Feature feature) {

        getFeatures().add(feature);
        Collections.sort(getFeatures(), new Comparator<Feature>() {
            @Override
            public int compare(Feature lhs, Feature rhs) {
                if (lhs.getTime() > rhs.getTime())
                    return 1;
                else
                    return -1;
            }
        });
        printFeaturesOnLog();
        Log.d("FEATURE_STACK", printFeatureStack());
        cleanUpFeatureStack();
    }

    @Override
    public double getTimeThresholdForGC() {
        return 30;
    }
}
