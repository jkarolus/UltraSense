package jakobkarolus.de.pulseradar.algorithm;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Jakob on 25.05.2015.
 */
public class AudioManager {

    private Context ctx;
    private static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "PulseRadar" + File.separator;

    private static final int SAMPLE_RATE = 44100;
    private static final double STD_FREQ = 20000;
    private DataOutputStream dos;
    private File tempFile;

    private AudioTrack at;
    private AudioRecord ar;

    private SignalGenerator signalGen;

    private boolean recordRunning = false;
    private double currentFreq;

    private static final int minSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);


    public AudioManager(Context ctx, SignalGenerator signalGen){
        this.ctx = ctx;
        new File(fileDir).mkdirs();

        this.signalGen = signalGen;
        at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10* minSize);

        currentFreq = STD_FREQ;
    }

    /**
     * called when pressing the "Start recording" button.<br>
     * Starts the record and play threads
     *
     * @throws FileNotFoundException
     */
    public void startRecord() throws FileNotFoundException {

        tempFile = new File(ctx.getExternalCacheDir().getAbsolutePath() + "/temp.raw");

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
                    byte[] audio =  signalGen.generateAudio();
                    at.write(audio, 0, audio.length);
                }
                at.flush();
            }
        });
        playThread.start();
    }

    /**
     * called when pressing the "Stop recording" button.<br>
     * Finishes play and record thread and ask for filename.<br>
     * Saved files can be found in the app-own directory (Android/data/...)
     *
     * @throws IOException
     */
    public void stopRecord() throws IOException {
        recordRunning = false;
        ar.stop();
        at.pause();
        at.flush();
        dos.flush();
        dos.close();

    }

    /**
     * saves the current recorded audio (if any) under the given filename
     *
     * @param waveFileName
     * @throws IOException
     */
    public void saveWaveFile(String waveFileName) throws IOException {

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
            File file = new File(fileDir + waveFileName + ".wav");
            output = new DataOutputStream(new FileOutputStream(file, false));
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

            MediaScannerConnection.scanFile(ctx,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        }

        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * converts recording byte stream into double array
     * @return double array of the latest recorded data
     */
    public double[] getRecordData() {

        int dataLength = (int) tempFile.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(tempFile));
            input.read(rawData);
            input.close();
        } catch (IOException e){
            //
        }

        double[] data = refineData(convertToDoubles(converToShorts(rawData)));
        return data;
    }

    private double[] refineData(double[] data) {
        //delete first and last second
        double[] refinedData = new double[Math.max(0,data.length-2*SAMPLE_RATE)];
        if(refinedData.length != 0)
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

    public boolean hasRecordData() {
        return tempFile != null && tempFile.length() > 0;
    }

    public void setSignalGenerator(SignalGenerator signalGenerator) {
        this.signalGen = signalGenerator;
    }
}
