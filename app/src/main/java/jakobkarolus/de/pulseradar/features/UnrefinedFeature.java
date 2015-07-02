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

    private boolean hasStarted;

    public UnrefinedFeature(){
        this.unrefinedFeature =  new Vector<>();
        this.hasStarted = false;
    }

    public UnrefinedFeature(List<Double> unrefinedFeature){
        this.unrefinedFeature =  new Vector<>();
        this.unrefinedFeature.addAll(unrefinedFeature);
        this.hasStarted = false;
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
}
