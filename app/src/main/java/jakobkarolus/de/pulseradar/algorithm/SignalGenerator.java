package jakobkarolus.de.pulseradar.algorithm;

/**
 * Created by Jakob on 27.05.2015.
 */
public interface SignalGenerator {

    /**
     * generates an audio signal conforming to app mode and parameters
     *
     * @return audio signal as byte[]
     */
    public byte[] generateAudio();
}
