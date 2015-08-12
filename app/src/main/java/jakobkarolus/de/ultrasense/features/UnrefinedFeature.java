package jakobkarolus.de.ultrasense.features;

import java.util.List;
import java.util.Vector;

/**
 * Data model for each FeatureDetector to keep track of the current "Feature state".<br>
 * Keeps track of frequency values given time-steps.<br>
 *
 * <br><br>
 * Created by Jakob on 02.07.2015.
 */
public class UnrefinedFeature {

    //used to calculate input data array for feature approximation
    private double timeIncreasePerStep;

    //per time-step list of extracted frequency values
    private List<Double> unrefinedFeature;

    private double startTime;
    private double endTime;

    private boolean hasStarted;

    /**
     * initiate a new UnrefinedFeature
     */
    public UnrefinedFeature(double timeIncreasePerStep){
        this.unrefinedFeature =  new Vector<>();
        this.hasStarted = false;
        this.timeIncreasePerStep = timeIncreasePerStep;
    }

    /**
     * copy constructor
     * @param current
     */
    public UnrefinedFeature(UnrefinedFeature current){
        this.unrefinedFeature =  new Vector<>();
        this.unrefinedFeature.addAll(current.getUnrefinedFeature());
        this.hasStarted = current.hasStarted();
        this.startTime = current.getStartTime();
        this.endTime = current.getEndTime();
        this.timeIncreasePerStep = current.getTimeIncreasePerStep();
    }

    /**
     * adds value for a single timestep to the UnrefinedFeature
     * @param value the value to add
     */
    public void addTimeStep(double value){
        this.unrefinedFeature.add(value);
    }

    /**
     * If working with slack steps, this method will remove any unnecessary slacks.
     * This can happen if slack is used but the feature ended nonetheless
     *
     * @param slackWidth
     * @return whether this feature contains any value after reduction, in other words: if it is valid
     */
    public boolean removeSlackedSteps(int slackWidth){
        for(int i=0; i < slackWidth; i++){
            int idx = this.unrefinedFeature.size()-1;
            if(idx >= 0)
                this.unrefinedFeature.remove(idx);
        }
        return !this.unrefinedFeature.isEmpty();
    }

    /**
     *
     * @return whether a UnrefinedFeature was started
     */
    public boolean hasStarted() {
        return hasStarted;
    }

    /**
     * change the state of an UnrefinedFeature
     * @param hasStarted the state to set
     */
    public void setHasStarted(boolean hasStarted) {
        this.hasStarted = hasStarted;
    }

    public List<Double> getUnrefinedFeature() {
        return unrefinedFeature;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public double getTimeIncreasePerStep() {
        return timeIncreasePerStep;
    }

    public void setTimeIncreasePerStep(double timeIncreasePerStep) {
        this.timeIncreasePerStep = timeIncreasePerStep;
    }
}
