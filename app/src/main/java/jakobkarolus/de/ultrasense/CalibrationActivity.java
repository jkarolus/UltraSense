package jakobkarolus.de.ultrasense;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import jakobkarolus.de.ultrasense.view.CalibrationFragment;

/**
 * Activity used for calibrating detection parameters by the user
 *
 * <br><br>
 * Created by Jakob on 17.08.2015.
 */
public class CalibrationActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CalibrationFragment())
                    .commit();
        }

    }
}
