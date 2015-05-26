package jakobkarolus.de.pulseradar.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import jakobkarolus.de.pulseradar.R;

/**
 * Created by Jakob on 25.05.2015.
 */
public class SettingsFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_MODE = "pref_mode";
    public static final String CW_MODE = "CW";
    public static final String FMCW_MODE = "FMCW";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SettingsFragment.PREF_MODE)){
            Preference prefMode = findPreference(key);
            prefMode.setSummary(sharedPreferences.getString(key, "CW"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
