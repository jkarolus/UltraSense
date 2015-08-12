package jakobkarolus.de.ultrasense.audio;

/**
 * Interface for all Signal generators used in UltraSense
 *
 * Created by Jakob on 27.05.2015.
 */
public interface SignalGenerator {

    /**
     * generates an audio signal conforming to app mode and parameters
     *
     * @return audio signal as byte[]
     */
    public byte[] generateAudio();

    /**
     *
     *
     * @return the carrier frequency for the generated signal
     */
    public double getCarrierFrequency();
}
