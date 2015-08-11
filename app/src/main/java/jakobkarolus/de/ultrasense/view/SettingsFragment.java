package jakobkarolus.de.ultrasense.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;

import jakobkarolus.de.ultrasense.R;

/**
 * Created by Jakob on 25.05.2015.
 */
public class SettingsFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_MODE = "pref_mode";
    public static final String CW_MODE = "CW";
    public static final String FMCW_MODE = "FMCW";
    private static final String FMCW_PARAS_KEY = "pref_key_fmcw_paras";
    private static final String CW_PARAS_KEY = "pref_key_cw_paras";

    public static final String KEY_FMCW_BOT_FREQ = "pref_key_fmcw_bottom_freq";
    public static final String KEY_FMCW_TOP_FREQ = "pref_key_fmcw_top_freq";
    public static final String KEY_FMCW_CHIRP_DUR = "pref_key_fmcw_chirp_duration";
    public static final String KEY_FMCW_CHIRP_CYCLES = "pref_key_fmcw_chirp_cycles";
    public static final String KEY_FMCW_RAMP_UP = "pref_key_fmcw_ramp_up";

    public static final String KEY_CW_FREQ = "pref_key_cw_freq";

    public static final String KEY_USE_PRECALIBRATION = "pref_key_use_precalibration";
    public static final String KEY_FFT_LENGTH = "pref_key_fft_length";
    public static final String KEY_HOPSIZE = "pref_key_hopsize";
    public static final String KEY_HALF_CARRIER_WIDTH = "pref_key_halfCarrierWidth";
    public static final String KEY_DB_THRESHOLD = "pref_key_db_threshold";
    public static final String KEY_HIGH_FEAT_THRESHOLD = "pref_key_feat_high_threshold";
    public static final String KEY_LOW_FEAT_THRESHOLD = "pref_key_feat_low_threshold";
    public static final String KEY_FEAT_SLACK = "pref_key_feat_slack";

    public static final String SETTINGS_WARNING_IGNORE = "pref_ignore_settings_warning";




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        togglePreferences();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean ignoreWarning = getPreferenceManager().getSharedPreferences().getBoolean(SETTINGS_WARNING_IGNORE, false);
        if(!ignoreWarning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Important!");
            builder.setMessage("Parameter changes only influence the Recording functionality!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //nothing to do here
                }
            });
            builder.show();
        }
    }

    private void togglePreferences() {
        String prefMode = getPreferenceManager().getSharedPreferences().getString(PREF_MODE, "");
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
        if(prefMode.equals(CW_MODE)){
            getPreferenceScreen().removePreference(findPreference(FMCW_PARAS_KEY));
        }
        else
            getPreferenceScreen().removePreference(findPreference(CW_PARAS_KEY));

        updateSummaries();
    }

    private void updateSummaries() {
        for(int i=0; i < getPreferenceScreen().getPreferenceCount(); i++){
            Preference p = getPreferenceScreen().getPreference(i);
            updateSummaryForPreference(p);
        }
    }

    private void updateSummaryForPreference(Preference p){
        if(p instanceof ListPreference){
            ListPreference lp = (ListPreference) p;
            lp.setSummary(lp.getValue());
        }
        if(p instanceof EditTextPreference){
            EditTextPreference ep = (EditTextPreference) p;
            ep.setSummary(ep.getText());
        }
        if(p instanceof PreferenceCategory){
            PreferenceCategory pc = (PreferenceCategory) p;
            for(int i=0; i < pc.getPreferenceCount(); i++)
                updateSummaryForPreference(pc.getPreference(i));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        togglePreferences();
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_compute_stft).setVisible(false);
        menu.findItem(R.id.action_show_last).setVisible(false);
        menu.findItem(R.id.action_test_detection).setVisible(false);
        menu.findItem(R.id.action_update_debug_info).setVisible(false);
    }

}
