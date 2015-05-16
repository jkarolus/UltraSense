package jakobkarolus.de.pulseradar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jakobkarolus.de.pulseradar.algorithm.AlgoHelper;
import jakobkarolus.de.pulseradar.algorithm.STFT;
import jakobkarolus.de.pulseradar.view.SpectrogramView;
import jakobkarolus.de.pulseradar.view.TouchImageView;


public class PulseRadar extends ActionBarActivity {

    private static int WINDOW_LENGTH = 4096;
    private static int HOP_SIZE = 1024;
    private static int NFFT = 4096;

    private static final int SAMPLE_RATE = 44100;
    private static final double STD_FREQ = 20000;
    private DataOutputStream dos;
    private File tempFile;

    private AudioTrack at;
    private AudioRecord ar;

    private boolean recordRunning = false;
    private double currentFreq;

    private Button startButton;
    private Button stopButton;
    private Button testButton;
    private View rootView;

    private double[][] currentSTFT;

    private static final int minSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_radar);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        at = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10* minSize);

        currentFreq = STD_FREQ;

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * called when pressing the "Start recording" button.<br>
     * Starts the record and play threads
     *
     * @param view
     * @throws FileNotFoundException
     */
    public void startRecord(View view) throws FileNotFoundException {

        startButton.setEnabled(false);
        startButton.setText("Recording...");
        startButton.setBackgroundColor(Color.RED);
        stopButton.setEnabled(true);


        tempFile = new File(PulseRadar.this.getExternalCacheDir().getAbsolutePath() + "/temp.raw");

        if(tempFile.exists())
            tempFile.delete();

        dos = new DataOutputStream(new FileOutputStream(tempFile));
        ar.startRecording();
        recordRunning = true;
        Thread recordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                short[] buffer = new short[minSize];
                while(recordRunning){
                    ar.read(buffer, 0, minSize);
                    try {
                        ByteBuffer bytes = ByteBuffer.allocate(buffer.length * 2);
                        for (short s : buffer) {
                            bytes.putShort(s);
                        }
                        dos.write(bytes.array());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        recordThread.start();


        at.play();

        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while(recordRunning){
                    byte[] audio = generateAudio(currentFreq, 0.1, 1.0);
                    at.flush();
                    at.write(audio, 0, audio.length);
                }
            }
        });
        playThread.start();
    }

    /**
     * called when pressing the "Stop recording" button.<br>
     * Finishes play and record thread and ask for filename.<br>
     * Saved files can be found in the app-own directory (Android/data/...)
     *
     * @param view
     * @throws IOException
     */
    public void stopRecord(View view) throws IOException {

        startButton.setEnabled(true);
        startButton.setText(R.string.button_start_record);
        startButton.setBackgroundResource(android.R.drawable.btn_default);
        stopButton.setEnabled(false);
        testButton.setEnabled(true);

        recordRunning = false;
        ar.stop();
        dos.flush();
        dos.close();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AskForFileNameDialog fileNameDialog = new AskForFileNameDialog();
        fileNameDialog.show(ft, "FileNameDialog");
    }

    public void testButton(View view) {
        new ComputeSTFTTask().execute(generateTestData());

    }

    private class ComputeSTFTTask extends AsyncTask<double[], Void, double[][]>{

        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(PulseRadar.this, "Computing STFT", "Please wait", true, false);
        }

        @Override
        protected void onPostExecute(double[][] stft) {
            currentSTFT = stft;
            /*
            try {
                FileWriter writer = new FileWriter(new File(PulseRadar.this.getExternalCacheDir().getAbsolutePath() + "/output.txt"), false);
                for(int i=0; i < currentSTFT.length; i++){
                    for(int j=0; j < currentSTFT[i].length; j++){
                        if(j == currentSTFT[i].length-1)
                            writer.write(currentSTFT[i][j] + ";\n");
                        else
                            writer.write(currentSTFT[i][j] + ",");
                    }
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            pd.dismiss();

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, new Spectrogram(), Spectrogram.class.getName());
            ft.addToBackStack(Spectrogram.class.getName());
            ft.commit();

        }

        @Override
        protected double[][] doInBackground(double[]... params) {
            STFT stftAlgo = new STFT(WINDOW_LENGTH, NFFT, HOP_SIZE, AlgoHelper.getHannWindow(WINDOW_LENGTH));
            return stftAlgo.computeSTFT(generateLatestData());
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            this.finish();
        }
    }

    @SuppressLint("ValidFragment")
    public class Spectrogram extends Fragment {

        public Spectrogram(){

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_spectrogram, container, false);
            TouchImageView spec = (TouchImageView) rootView.findViewById(R.id.spectrogram);
            SpectrogramView view =new SpectrogramView(getApplicationContext(), convertToGreyscale(currentSTFT));
            spec.setImageBitmap(view.getBitmap());
            return rootView;
        }
    }

    private double[] generateLatestData() {

        int dataLength = (int) tempFile.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(tempFile));
            input.read(rawData);
            input.close();
        } catch (IOException e){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PulseRadar.this, "Cannot access latest recording!", Toast.LENGTH_LONG).show();
                }
            });
        }

        double[] data = refineData(convertToDoubles(converToShorts(rawData)));
        return data;
    }

    private double[] refineData(double[] data) {
        //delete first and last second
        double[] refinedData = new double[data.length-2*SAMPLE_RATE];
        System.arraycopy(data, SAMPLE_RATE, refinedData, 0, data.length-2*SAMPLE_RATE);
        return refinedData;
    }

    private short[] converToShorts(byte[] rawData) {
        short[] shorts = new short[rawData.length /2];
        ByteBuffer bb = ByteBuffer.wrap(rawData);
        for(int i=0; i < shorts.length; i++){
            shorts[i] = bb.getShort();
        }
        return shorts;
    }

    private double[] convertToDoubles(short[] shorts) {
        double[] data = new double[shorts.length];
        for(int i=0; i < shorts.length; i++){
            data[i] = shorts[i];
        }
        return data;
    }

    private int[][] convertToGreyscale(double[][] stft) {
        double maxValue = findMax(stft, false);
        double minValue = findMin(stft, false);
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

    private int[][] convertToHeatmap(double[][] stft) {
        double maxValue = findMax(stft, true);
        int[][] spec = new int[stft.length][stft[0].length];
        for (int i = 0; i < stft.length; i++) {
            for (int j = 0; j < stft[i].length; j++) {
                //int value = 255 - (int) ((stft[i][j]/maxValue)*255.0);

                double ratio = 2 * stft[i][j] /maxValue;

                int b = (int) Math.round(Math.max(0.0, 255.0*(1.0 - ratio)));
                int r = (int) Math.round(Math.max(0.0, 255.0 * (ratio - 1.0)));
                int g = 255 - b - r;
                //spec[i][j] = Color.rgb(value, value, value);
                spec[i][j] = Color.rgb(r, g, b);
            }
        }
        return spec;
    }

    private double findMax(double[][] stft, boolean topHalf) {
        double currentMax = Double.MIN_VALUE;
        for(int i=0; i < stft.length; i++){
            for(int j=(topHalf ? stft[i].length/2 : 0); j < stft[i].length; j++){
                if(stft[i][j] > currentMax)
                    currentMax = stft[i][j];
            }
        }
        return currentMax;
    }

    private double findMin(double[][] stft, boolean topHalf) {
        double currentMin = Double.MAX_VALUE;
        for(int i=0; i < stft.length; i++){
            for(int j=(topHalf ? stft[i].length/2 : 0); j < stft[i].length; j++){
                if(stft[i][j] < currentMin)
                    currentMin = stft[i][j];
            }
        }
        return currentMin;
    }

    private double[] generateTestData() {
        double sampleRate = 44100.0;
        double amplitude = 1.0;
        double frequency = 5000.0;

        double seconds = 1.0;
        double[] buffer = new double[(int) (5*seconds * sampleRate)];


        for(int i=0; i < 5; i++)
        {
            for (int sample = (int)(i*seconds*sampleRate); sample < (int)((i+1)*seconds*sampleRate); sample++) {
                double time = sample / sampleRate;
                buffer[sample] = (amplitude * Math.sin(i*frequency*2.0*Math.PI * time));
            }
        }
        return buffer;
    }


    @SuppressLint("ValidFragment")
    public class AskForFileNameDialog extends DialogFragment{

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

                            //EditText fileName = (EditText) view
                              //      .findViewById(R.id.input_filename_record);

                            try {
                                saveWaveFile(fileName.getText().toString());
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


    private void saveWaveFile(String waveFileName) throws IOException {

        int dataLength = (int) tempFile.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(tempFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(new File(PulseRadar.this.getExternalCacheDir().getAbsolutePath() + "/" + waveFileName + ".wav")));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + dataLength); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, dataLength); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        }

        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * causes the play thread to generate a signal of a specific frequency for 500ms to discern events
     * @param view
     */
    public void playSignal(View view){

        currentFreq = 19000.0;

        final Button signalButton = (Button) view;
        signalButton.setText("Playing Signal...");

        CountDownTimer timer = new CountDownTimer(500, 500) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                currentFreq = STD_FREQ;
                signalButton.setText(R.string.button_play_signal);
            }
        };
        timer.start();

    }

    private byte[] generateAudio(double frequency, double seconds, double amplitude){

        float[] buffer = new float[(int) (seconds * SAMPLE_RATE)];


        for (int sample = 0; sample < buffer.length; sample++) {
            double time = sample / (double) SAMPLE_RATE;

            //if(sample == buffer.length/2)
            //  buffer[sample] = 1.0f;
            buffer[sample] = (float) (amplitude * Math.sin(frequency*2.0*Math.PI * time));
        }

        final byte[] byteBuffer = new byte[buffer.length * 2];
        int bufferIndex = 0;
        for (int i = 0; i < byteBuffer.length; i++) {
            final int x = (int) (buffer[bufferIndex++] * 32767.0);
            byteBuffer[i] = (byte) x;
            i++;
            byteBuffer[i] = (byte) (x >>> 8);
        }

        return byteBuffer;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_pulse_radar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    @SuppressLint("ValidFragment")
    public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_pulse_radar, container, false);
            startButton = (Button) rootView.findViewById(R.id.button_start_record);
            stopButton = (Button) rootView.findViewById(R.id.button_stop_record);
            testButton = (Button) rootView.findViewById(R.id.button_fft);
            return rootView;
        }
    }
}
