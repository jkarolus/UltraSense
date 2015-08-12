package jakobkarolus.de.ultrasense.features;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import jakobkarolus.de.ultrasense.R;
import jakobkarolus.de.ultrasense.features.gestures.GestureCallback;
import jakobkarolus.de.ultrasense.features.gestures.GestureFP;
import jakobkarolus.de.ultrasense.view.UltraSenseFragment;

/**
 * <br><br>
 * Created by Jakob on 26.07.2015.
 */
public class TestDataGestureFP extends GestureFP {

    private List<Feature> extractedFeatures;

    private Activity ctx;

    public TestDataGestureFP(GestureCallback gestureCallback, Activity ctx) {
        super(gestureCallback);
        this.ctx = ctx;
        this.extractedFeatures = new Vector<>();
    }

    @Override
    public void closeFeatureWriter() {
        super.closeFeatureWriter();

        //but also save a comparison between extracted features and testData
        List<Feature> testFeatures = new Vector<>();
        try {
            Scanner outerScanner = new Scanner(new File(UltraSenseFragment.fileDir + "testing/up_down_s3_features.txt"));
            while(outerScanner.hasNext()){
                Scanner scan = new Scanner(outerScanner.next());
                scan.useDelimiter(",");
                double mu = Double.parseDouble(scan.next());
                double sigma = Double.parseDouble(scan.next());
                double weight = Double.parseDouble(scan.next());
                testFeatures.add(new GaussianFeature(0, mu, sigma, weight));
                scan.close();
            }
            outerScanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        compareFeatures(testFeatures, extractedFeatures);
    }

    private void compareFeatures(List<Feature> testFeatures, List<Feature> extractedFeatures) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Feature comparison report");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        if(testFeatures.size() != extractedFeatures.size()){
            builder.setMessage("Amount of features is not the same. Check feat.txt for details!");
            builder.show();
            return;
        }

        double meanDiff = 0.0;
        double stdDiff = 0.0;
        double weightDiff = 0.0;

        for(int i=0; i < testFeatures.size(); i++){
            Feature f1 = testFeatures.get(i);
            Feature f2 = extractedFeatures.get(i);
            meanDiff += Math.abs(f1.getTime()-f2.getTime());
            stdDiff += Math.abs(f1.getLength()-f2.getLength());
            weightDiff += Math.abs(f1.getWeight()-f2.getWeight());
        }

        DecimalFormat df = new DecimalFormat("0.000000");
        builder.setMessage("Mean Diff:\t" + df.format(meanDiff) + "\nStd Diff:\t" + df.format(stdDiff) + "\nWeight Diff:\t" + df.format(weightDiff));
        builder.show();
    }

    @Override
    public void processFeature(Feature feature) {
        //still save it to the file
        super.saveFeatureToFile(feature);

        //but also save it to memory for easy comparison
        extractedFeatures.add(feature);
    }
}
