package jakobkarolus.de.pulseradar.features;

/**
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFeature implements Feature{

    private double mu;
    private double sigma;
    private double weight;


    public GaussianFeature(double mu, double sigma, double weight) {
        this.mu = mu;
        this.sigma = sigma;
        this.weight = weight;
    }


    public double getMu() {
        return mu;
    }

    public double getSigma() {
        return sigma;
    }

    public double getWeight() {
        return weight;
    }
}
