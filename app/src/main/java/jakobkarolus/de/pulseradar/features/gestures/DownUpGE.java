package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Created by Jakob on 08.07.2015.
 */
public class DownUpGE implements GestureExtractor{

    //fast: 0.3 <-> 0.4
    //slow: 0.4 <-> 0.6
    private static final double UP_DOWN_THRESHOLD_HIGH = 0.6;
    private static final double UP_DOWN_THRESHOLD_LOW = 0.3;


    @Override
    public List<Gesture> detectGesture(List<Feature> features) {
        List<Gesture> gestures = new Vector<>();
        ListIterator<Feature> iter = features.listIterator();
        while(iter.hasNext()){
            Feature f = iter.next();
            if(isDownGesture(f)){
                if(iter.hasNext()){
                    Feature next = iter.next();
                    if(isUpGesture(next)){
                        //check if they are close enough
                        double distance = next.getTime()-f.getTime();
                        if(distance <= getHighThreshold() && distance >= getLowThreshold()){
                            gestures.add(getGesture());
                            iter.remove();
                            iter.previous();
                            iter.remove();
                        }
                    }
                }
            }
        }
        return gestures;
    }

    @Override
    public boolean calibrate(List<Feature> features) {
        //TODO:implement
        return false;
    }

    protected boolean isUpGesture(Feature f) {
        if(f.getLength() >= 0.05 && f.getLength() <= 0.16) {
            if (f.getWeight() >= -4.5 && f.getWeight() <= -2.5) {
                return true;
            }
        }
        return false;
    }

    protected boolean isDownGesture(Feature f) {
        if(f.getLength() >= 0.05 && f.getLength() <= 0.16) {
            if (f.getWeight() >= 2.5 && f.getWeight() <= 4.5) {
                return true;
            }
        }
        return false;
    }

    protected double getHighThreshold(){
        return UP_DOWN_THRESHOLD_HIGH;
    }

    protected double getLowThreshold(){
        return UP_DOWN_THRESHOLD_LOW;
    }

    protected Gesture getGesture(){
        return Gesture.DOWN_UP;
    }
}
