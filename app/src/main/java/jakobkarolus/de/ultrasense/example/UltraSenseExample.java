package jakobkarolus.de.ultrasense.example;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import jakobkarolus.de.ultrasense.R;

/**
 * Example activity showing scenarios of UltraSense
 * <br><br>
 * Created by Jakob on 10.08.2015.
 */
public class UltraSenseExample extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_radar);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new UltraSenseExampleFragment())
                    .commit();
        }

    }
}
