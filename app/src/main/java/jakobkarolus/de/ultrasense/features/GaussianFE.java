package jakobkarolus.de.ultrasense.features;

import java.util.List;

/**
 * A FeatureExtractor implementing linear least-square approx using a Gaussian curve
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFE extends FeatureExtractor{


    /**
     * creates a new GaussianFE with the given id.<br>
     * The id used to discern different FEs when passing their feature to the FeatureProcessor.
     *
     * @param id identifier for this specific FeatureExtractor
     */
    public GaussianFE(int id) {
        super(id);
    }

    @Override
    public Feature onHighFeatureDetected(UnrefinedFeature uF) {

        return fitGaussian(uF);

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
            return null;
        }

        if(Math.abs(weight - 0.0) < 1e-4)
            return null;

        return new GaussianFeature(getId(), mu, sigma, weight);
    }

    @Override
    public Feature onLowFeatureDetected(UnrefinedFeature uF) {

        GaussianFeature gf = fitGaussian(uF);
        if(gf != null)
            return new GaussianFeature(gf.getExtractorId(), gf.getTime(), gf.getLength(), gf.getWeight()*(-1));
        else
            return null;

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
