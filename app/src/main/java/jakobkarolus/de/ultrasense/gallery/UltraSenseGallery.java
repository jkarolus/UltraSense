package jakobkarolus.de.ultrasense.gallery;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import jakobkarolus.de.ultrasense.R;

/**
 * Example activity showing picture gallery scenario of UltraSense
 * <br><br>
 * Created by Jakob on 06.09.2015.
 */
public class UltraSenseGallery extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_radar);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new UltraSenseGalleryFragment())
                    .commit();
        }

    }
}
