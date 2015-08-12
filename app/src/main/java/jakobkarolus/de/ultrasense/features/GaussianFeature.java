package jakobkarolus.de.ultrasense.features;

/**
 * Feature represented by a Gaussian curve
 *<br><br>
 * Created by Jakob on 02.07.2015.
 */
public class GaussianFeature extends Feature{

    private double mu;
    private double sigma;
    private double weight;


    public GaussianFeature(int extractorId, double mu, double sigma, double weight) {
        super(extractorId);
        this.mu = mu;
        this.sigma = sigma;
        this.weight = weight;
    }

    @Override
    public double getLength() {
        return sigma;
    }

    @Override
    public double getTime() {
        return mu;
    }

    @Override
    public double getWeight() {
        return weight;
    }

}
