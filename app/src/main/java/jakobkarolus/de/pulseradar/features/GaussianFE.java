package jakobkarolus.de.pulseradar.features;

import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import jakobkarolus.de.pulseradar.view.PulseRadarFragment;

/**
 * A FeatureExtractor implementing linear least-square approx using a Gaussian curve
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFE extends FeatureExtractor{


    public GaussianFE(FeatureProcessor featProc) {
        super(featProc);
    }

    @Override
    public void onHighFeatureDetected(UnrefinedFeature uF) {

        GaussianFeature gf = fitGaussian(uF);
        if(gf != null)
            getFeatProcessor().processFeature(fitGaussian(uF));

    }

    private GaussianFeature fitGaussian(UnrefinedFeature uF){

        if(uF.getEndTime() -uF.getStartTime() <= 0)
            return null;

        if(uF.getUnrefinedFeature().size() <= 1)
            return null;

        double[] y = toArray(uF.getUnrefinedFeature());

        double[] x = getRange(uF.getStartTime(), y.length, uF.getTimeIncreasePerStep());
        double mu = (x[x.length-1] + x[0]) / 2.0;

        double variance = 0.0;
        for(double d: x){
            variance += ((d-mu)*(d-mu));
        }
        variance /= uF.getUnrefinedFeature().size();
        double sigma = Math.sqrt(variance);

        x = normpdf(x, mu, sigma);
        double innerProd1 = innerProduct(x, x) + 1e-6;
        double innerProd2 = innerProduct(x, y);
        double weight = (1.0/innerProd1)*innerProd2;

        if(Double.isNaN(weight)){
            Log.e("GFE", "NaN");
        }

        if(Math.abs(weight-3.42311923) <= 1e-3) {
            try {
                FileWriter writer = new FileWriter(PulseRadarFragment.fileDir + "last_feat.txt");
                writer.write("x: ");
                for (double d : x) {
                    writer.write("" + d + ",");
                }
                writer.flush();

                writer.write("\ny: ");
                for (double d : y) {
                    writer.write("" + d + ",");
                }
                writer.flush();
                writer.write("\nX*X': " + innerProd1);
                writer.write("\nX*y: " + innerProd2);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return new GaussianFeature(mu, sigma, weight);
    }

    @Override
    public void onLowFeatureDetected(UnrefinedFeature uF) {

        GaussianFeature gf = fitGaussian(uF);
        if(gf != null)
            getFeatProcessor().processFeature(new GaussianFeature(gf.getTime(), gf.getLength(), gf.getWeight()*(-1)));

    }


    private double[] getRange(double start, int count, double delta) {

        double[] range = new double[count];
        for(int i=0; i < count; i++)
            range[i] = start + i*delta;
        return range;
    }

    private double[] toArray(List<Double> list) {
        double[] array = new double[list.size()];
        for(int i=0; i < array.length; i++)
            array[i] = list.get(i);
        return array;
    }


    private double[] normpdf(double[] x, double mu, double sigma){
        double[] y = new double[x.length];
        for(int i=0; i < x.length; i++){
            y[i] = Math.exp(-0.5*Math.pow((x[i]-mu)/sigma, 2.0)) / (Math.sqrt(2.0*Math.PI)*sigma);
        }
        return y;
    }

    private double innerProduct(double[] vec1, double[] vec2){
        double innerProdut = 0.0;
        for(int i=0; i < vec1.length; i++){
            innerProdut += vec1[i]*vec2[i];
        }
        return innerProdut;
    }
}
