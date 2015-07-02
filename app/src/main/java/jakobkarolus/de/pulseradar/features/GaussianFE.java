package jakobkarolus.de.pulseradar.features;

import java.util.List;

/**
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFE extends FeatureExtractor{


    public GaussianFE(FeatureProcessor featProc) {
        super(featProc);
    }

    @Override
    public void onFeatureDetected(UnrefinedFeature uF) {

        double mu = (double) (uF.getUnrefinedFeature().size()-1)/2.0;
        double variance = 0.0;
        for(Double d: uF.getUnrefinedFeature()){
            variance += ((d-mu)*(d-mu));
        }
        variance /= uF.getUnrefinedFeature().size();
        double sigma = Math.sqrt(variance);
        double[] y = toArray(uF.getUnrefinedFeature());
        double[] x = getRange(0, uF.getUnrefinedFeature().size());
        double innerProd1 = innerProduct(x, x) + 1e-6;
        double innerProd2 = innerProduct(x, y);
        double weight = (1.0/innerProd1)*innerProd2;

        GaussianFeature gf = new GaussianFeature(mu, sigma, weight);
        getFeatProc().processFeature(gf);

    }


    private double[] getRange(int start, int end) {
        double[] range = new double[end-1];
        for(int i=0; i < end; i++)
            range[i] = i;
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
            y[i] = Math.exp(-0.5*Math.pow((x[i]-mu)/sigma, 2)) / (Math.sqrt(2*Math.PI)*sigma);
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
