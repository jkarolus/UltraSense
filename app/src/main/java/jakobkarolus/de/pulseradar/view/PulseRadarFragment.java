package jakobkarolus.de.pulseradar.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import jakobkarolus.de.pulseradar.R;
import jakobkarolus.de.pulseradar.algorithm.AudioManager;
import jakobkarolus.de.pulseradar.algorithm.CWSignalGenerator;
import jakobkarolus.de.pulseradar.algorithm.FMCWSignalGenerator;
import jakobkarolus.de.pulseradar.algorithm.SignalGenerator;
import jakobkarolus.de.pulseradar.algorithm.StftManager;

/**
 * Created by Jakob on 25.05.2015.
 */
public class PulseRadarFragment extends Fragment{

    private static final String DISPLAY_LAST_SPEC = "DISPLAY_LAST_SPEC";
    private Bitmap lastSpectrogram;
    private static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "PulseRadar" + File.separator;


    private AudioManager audioManager;
    private StftManager stftManager;

    private Button startButton;
    private Button stopButton;
    private Button computeStftButton;
    private Button showLastSpec;
    private View rootView;

    private String prefMode;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = new AudioManager(getActivity(), new CWSignalGenerator(20000, 0.1, 1.0, 44100));
        stftManager = new StftManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pulse_radar, container, false);
        startButton = (Button) rootView.findViewById(R.id.button_start_record);
        stopButton = (Button) rootView.findViewById(R.id.button_stop_record);
        computeStftButton = (Button) rootView.findViewById(R.id.button_fft);
        showLastSpec = (Button) rootView.findViewById(R.id.button_last_spec);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startRecord();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    stopRecord();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        computeStftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                computeStft();
            }
        });
        showLastSpec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLastSpec();
            }
        });

        return rootView;
    }

    private void showLastSpec() {
        if(lastSpectrogram != null) {
            Bundle args = new Bundle();
            args.putBoolean(DISPLAY_LAST_SPEC, true);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment spec = new Spectrogram();
            spec.setArguments(args);
            ft.replace(R.id.container, spec, Spectrogram.class.getName());
            ft.addToBackStack(Spectrogram.class.getName());
            ft.commit();
        }
        else
            Toast.makeText(getActivity(), "No latest spectrogram available", Toast.LENGTH_LONG).show();
    }


    private void computeStft() {
        if(audioManager.hasRecordData()) {
            new ComputeSTFTTask().execute();
        }
        else{
            Toast.makeText(getActivity(), "No latest record available", Toast.LENGTH_LONG).show();
        }
    }

    private void startRecord() throws FileNotFoundException {
        startButton.setEnabled(false);
        startButton.setText("Recording...");
        startButton.setBackgroundColor(Color.RED);
        stopButton.setEnabled(true);
        audioManager.startRecord();
    }

    private void stopRecord() throws IOException {

        startButton.setEnabled(true);
        startButton.setText(R.string.button_start_record);
        startButton.setBackgroundResource(android.R.drawable.btn_default);
        stopButton.setEnabled(false);
        computeStftButton.setEnabled(true);
        audioManager.stopRecord();


        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AskForFileNameDialog fileNameDialog = new AskForFileNameDialog();
        fileNameDialog.show(ft, "FileNameDialog");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        audioManager.setSignalGenerator(getSignalGeneratorForMode(PreferenceManager.getDefaultSharedPreferences(getActivity())));
    }

    private SignalGenerator getSignalGeneratorForMode(SharedPreferences sharedPreferences) {
        String mode = sharedPreferences.getString(SettingsFragment.PREF_MODE, "CW");
        if(mode.equals(SettingsFragment.FMCW_MODE)) {
            //TODO: add custom freq and stuff
            return new FMCWSignalGenerator(20000, 19000, 0.1, 20.0, 44100, 1.0f, false);
        }
        else {
            return new CWSignalGenerator(20000, 0.1, 1.0, 44100);
        }
    }


    private class ComputeSTFTTask extends AsyncTask<Void, String, Void> {

        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(getActivity(), "Computing STFT", "Please wait", true, false);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            pd.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pd.dismiss();

            if(stftManager.getCurrentSTFT() != null) {
                Bundle args = new Bundle();
                args.putBoolean(DISPLAY_LAST_SPEC, false);

                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment spec = new Spectrogram();
                spec.setArguments(args);
                ft.replace(R.id.container, spec, Spectrogram.class.getName());
                ft.addToBackStack(Spectrogram.class.getName());
                ft.commit();
            }
            else{
                Toast.makeText(getActivity(), "Sequence too short", Toast.LENGTH_LONG).show();
            }
        }


        @Override
        protected Void doInBackground(Void... params) {
            double[] data = audioManager.getRecordData();
            if(data.length != 0) {
                stftManager.setData(data);
                publishProgress("Applying high pass filter");
                stftManager.applyHighPassFilterOld();
                publishProgress("Modulating signal");
                stftManager.modulate(19000);
                publishProgress("Downsampling");
                stftManager.downsample(4);
                publishProgress("STFT");
                stftManager.computeSTFT();
            }
            return null;
        }
    }

    private void saveDataToFile(String fileName, double[] data) {
        try {
            FileWriter writer = new FileWriter(new File(fileDir + fileName  + ".txt"), false);
            for(int i=0; i < data.length; i++){
                writer.write(data[i] + " ");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveStftToFile(String fileName, double[][] stft) {

        try {
            FileWriter writer = new FileWriter(new File(fileDir + fileName  + ".txt"), false);
            for(int i=0; i < stft.length; i++){
                for(int j=0; j < stft[i].length; j++){
                    if(j == stft[i].length-1)
                        writer.write(stft[i][j] + ";\n");
                    else
                        writer.write(stft[i][j] + ",");
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("ValidFragment")
    public class AskForFileNameDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_record_name,	null);
            final EditText fileName = (EditText) view.findViewById(R.id.input_filename_record);
            fileName.setText("test");
            builder.setView(view);
            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                audioManager.saveWaveFile(fileName.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
            builder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AskForFileNameDialog.this.getDialog().cancel();
                        }
                    });

            return builder.create();
        }
    }

    @SuppressLint("ValidFragment")
    public class Spectrogram extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            menu.findItem(R.id.action_settings).setVisible(false);
        }

        public Spectrogram(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_spectrogram, container, false);
            TouchImageView spec = (TouchImageView) rootView.findViewById(R.id.spectrogram);

            if(!getArguments().getBoolean(DISPLAY_LAST_SPEC))
                lastSpectrogram = createBitmap(convertToGreyscale(stftManager.getCurrentSTFT()));
            spec.setImageBitmap(lastSpectrogram);

            return rootView;
        }


        private Bitmap createBitmap(int[][] data){
            int height = data[0].length;
            int width = data.length;
            //Toast.makeText(PulseRadar.this, "#frequencies: " + height + ", #timesteps: " + width, Toast.LENGTH_LONG).show();

            int widthFactor = 16;
            int[] arrayCol = new int[width*height*widthFactor/2];
            int counter = 0;
            for(int i = height/2-1; i >= 0; i--) {
                for(int j = 0; j < width; j++) {

                    for(int k=0; k < widthFactor; k++)
                        arrayCol[counter+k] = data[j][i];

                    counter+=widthFactor;
                }
            }
            return Bitmap.createBitmap(arrayCol, width * widthFactor, height / 2, Bitmap.Config.ARGB_8888);
        }
    }

    private int[][] convertToGreyscale(double[][] stft) {
        double[] maxMin = findMaxMin(stft);
        double maxValue = maxMin[0];
        double minValue = maxMin[1];
        int[][] spec = new int[stft.length][stft[0].length];
        for (int i = 0; i < stft.length; i++) {
            for (int j = 0; j < stft[i].length; j++) {
                int value = Math.max(0, Math.min(255, (int) (((stft[i][j] - minValue) / (maxValue - minValue)) * 255.0)));
                value = 255-value;
                spec[i][j] = Color.rgb(value, value, value);
            }
        }
        return spec;
    }

    /**
     *
     * @param stft current STFT
     * @return two entry double array; [0] is max, [1] is min
     */
    private double[] findMaxMin(double[][] stft) {
        double currentMax = Double.MIN_VALUE;
        double currentMin = Double.MAX_VALUE;

        for(int i=0; i < stft.length; i++){
            for(int j=0; j < stft[i].length; j++){
                if(stft[i][j] > currentMax)
                    currentMax = stft[i][j];
                if(stft[i][j] < currentMin)
                    currentMin = stft[i][j];
            }
        }
        double[] maxMin = {currentMax, currentMin};
        return maxMin;
    }
}