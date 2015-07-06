package jakobkarolus.de.pulseradar.features;

import java.util.List;
import java.util.Vector;

/**
 * Keeps track of frequency values given time-steps.<br>
 * Used by FeatureDetectors to incremently build up a complete UnrefinedFeature
 * Created by Jakob on 02.07.2015.
 */
public class UnrefinedFeature {

    //per time-step list of extracted frequency values
    private List<Double> unrefinedFeature;

    private long startTime;
    private long endTime;

    private boolean hasStarted;

    public UnrefinedFeature(){
        this.unrefinedFeature =  new Vector<>();
        this.hasStarted = false;
    }

    public UnrefinedFeature(UnrefinedFeature current){
        this.unrefinedFeature =  new Vector<>();
        this.unrefinedFeature.addAll(current.getUnrefinedFeature());
        this.hasStarted = current.hasStarted();
        this.startTime = current.getStartTime();
        this.endTime = current.getEndTime();
    }

    public void addTimeStep(double value){
        this.unrefinedFeature.add(value);
    }

    public void addTimeSteps(List<Double> values){
        this.unrefinedFeature.addAll(values);
    }

    public boolean removeSlackedSteps(int slackWidth){
        for(int i=0; i < slackWidth; i++){
            int idx = this.unrefinedFeature.size()-1;
            if(idx >= 0)
                this.unrefinedFeature.remove(idx);
        }
        return !this.unrefinedFeature.isEmpty();
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public void setHasStarted(boolean hasStarted) {
        this.hasStarted = hasStarted;
    }

    public List<Double> getUnrefinedFeature() {
        return unrefinedFeature;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
