package jakobkarolus.de.pulseradar.features.gestures;

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import jakobkarolus.de.pulseradar.features.Feature;

/**
 * GestureExtractor recognizing downwards Gestures
 * <br><br>
 * Created by Jakob on 08.07.2015.
 */
public class DownGE implements GestureExtractor{
    @Override
    public List<Gesture> detectGesture(List<Feature> features) {
        List<Gesture> gestures = new Vector<>();
        ListIterator<Feature> iter = features.listIterator();
        while(iter.hasNext()){
            Feature f = iter.next();
            if(f.getLength() >= 1.5 && f.getLength() <= 3.5) {
                if (f.getWeight() >= 55.0) {
                    gestures.add(Gesture.DOWN);
                    iter.remove();
                }
            }
        }
        return gestures;
    }
}
