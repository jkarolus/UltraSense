package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * Created by Jakob on 08.07.2015.
 */
public class DownUpGE implements GestureExtractor{

    //fast: 10.5 <-> 12.5
    //slow: 7.5 <-> 8
    private static final double UP_DOWN_THRESHOLD_HIGH = 13.0;
    private static final double UP_DOWN_THRESHOLD_LOW = 7.0;


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

    protected boolean isUpGesture(Feature f) {
        if(f.getLength() >= 1.5 && f.getLength() <= 3.5) {
            if (f.getWeight() <= -60.0) {
                return true;
            }
        }
        return false;
    }

    protected boolean isDownGesture(Feature f) {
        if(f.getLength() >= 1.5 && f.getLength() <= 3.5) {
            if (f.getWeight() >= 55.0) {
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
