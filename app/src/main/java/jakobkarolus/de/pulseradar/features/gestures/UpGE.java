package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * GestureExtractor recognizing upwards Gestures
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public class UpGE implements GestureExtractor{


    @Override
    public List<Gesture> detectGesture(List<Feature> features) {
        List<Gesture> gestures = new Vector<>();
        ListIterator<Feature> iter = features.listIterator();
        while(iter.hasNext()){
            Feature f = iter.next();
            if(f.getLength() >= 1.5 && f.getLength() <= 3.5) {
                if (f.getWeight() <= -60.0) {
                    gestures.add(Gesture.UP);
                    iter.remove();
                }
            }
        }
        return gestures;
    }
}
