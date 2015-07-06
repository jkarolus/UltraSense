package jakobkarolus.de.pulseradar.audio;

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

import jakobkarolus.de.pulseradar.algorithm.AlgoHelper;
import jakobkarolus.de.pulseradar.algorithm.SignalGenerator;
import jakobkarolus.de.pulseradar.features.FeatureDetector;
import jakobkarolus.de.pulseradar.features.GaussianFE;
import jakobkarolus.de.pulseradar.features.MeanBasedFD;
import jakobkarolus.de.pulseradar.view.PulseRadarFragment;

/**
 * Created by Jakob on 25.05.2015.
 */
public class AudioManager{

    private Context ctx;
    private PulseRadarFragment thiz;
    private static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "PulseRadar" + File.separator;

    private static final int SAMPLE_RATE = 44100;
    private static final double STD_FREQ = 20000;
    private DataOutputStream dosSend;
    private DataOutputStream dosRec;
    private File tempFileRec;
    private File tempFileSend;

    private AudioTrack at;
    private AudioRecord ar;

    private SignalGenerator signalGen;

    private boolean recordRunning = false;
    private double currentFreq;

    private FeatureDetector featureDetector;

    //force a multiple of 4096
    private static final int minSize = 4*4096;//AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);


    public AudioManager(Context ctx, PulseRadarFragment thiz){
        this.ctx = ctx;
        this.thiz = thiz;
        new File(fileDir).mkdirs();
        currentFreq = STD_FREQ;
    }

    /**
     * called when pressing the "Start recording" button.<br>
     * Starts the record and play threads
     *
     * @throws FileNotFoundException
     */
    public void startRecord() throws FileNotFoundException {

        at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10* minSize);

        double carrierIdx = ((20000.0/22050.0)*2049)-1;
        featureDetector = new MeanBasedFD(4096, 2048, carrierIdx, 4, -50, 3, 2, 0, AlgoHelper.getHannWindow(4096));
        //TODO: fix reference
        featureDetector.registerFeatureExtractor(new GaussianFE(thiz));

        tempFileRec = new File(ctx.getExternalCacheDir().getAbsolutePath() + "/temp_rec.raw");
        tempFileSend = new File(ctx.getExternalCacheDir().getAbsolutePath() + "/temp_send.raw");

        if(tempFileRec.exists())
            tempFileRec.delete();

        if(tempFileSend.exists())
            tempFileSend.delete();

        dosRec = new DataOutputStream(new FileOutputStream(tempFileRec));
        dosSend = new DataOutputStream(new FileOutputStream(tempFileSend));


        final byte[] audio =  signalGen.generateAudio();

        Thread recordThread = new Thread(new Runnable() {

            private int sampleCounter;
            @Override
            public void run() {

                short[] buffer = new short[minSize];
                while(recordRunning){
                    int samplesRead = ar.read(buffer, 0, minSize);
                    writeShortBufferToStream(buffer, dosRec);

                    double[] bufferDouble = new double[minSize];
                    for(int i=0; i < minSize && i < samplesRead; i++){
                        bufferDouble[i] = (double) buffer[i] / 32768.0;
                    }

                    sampleCounter+=samplesRead;
                    //omit first second

                    if(sampleCounter >= 44100) {
                        featureDetector.checkForFeatures(bufferDouble);
                    }
                }

                try {
                    dosRec.flush();
                    dosRec.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                at.play();
                while(recordRunning){
                    at.write(audio, 0, audio.length);
                    writeByteBufferToStream(audio, dosSend);
                }
                ar.stop();
            }
        });

        ar.startRecording();
        recordRunning = true;
        recordThread.start();
        playThread.start();

    }

    /*
    private double[] convertToDouble(byte[] buffer) {


        double[] bufferDouble = new double[buffer.length/2];
        final int bytesPerSample = 2; // As it is 16bit PCM
        final double amplification = 1.0; // choose a number as you like
        for (int index = 0, floatIndex = 0; index < bytesRecorded - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = bufferData[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            micBufferData[floatIndex] = sample32;
        }



    }
*/

    private void writeByteBufferToStream(byte[] buffer, DataOutputStream dos){

        try{
            ByteBuffer bytes = ByteBuffer.allocate(buffer.length);
            for(int i=0; i < buffer.length; i+=2){
                byte byte1 = buffer[i];
                byte byte2 = buffer[i+1];
                short newshort = (short) ((byte2 << 8) + (byte1&0xFF));
                bytes.putShort(newshort);
            }
            dos.write(bytes.array());
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void writeShortBufferToStream(short[] buffer, DataOutputStream dos) {
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

    /**
     * called when pressing the "Stop recording" button.<br>
     * Finishes play and record thread and ask for filename.<br>
     * Saved files can be found in the app-own directory (Android/data/...)
     *
     * @throws IOException
     */
    public void stopRecord() throws IOException {
        recordRunning = false;
    }

    /**
     * saves the current recorded audio (if any) under the given filename.<br>
     * Also saves a copy of the send audio data.
     *
     * @param waveFileName
     * @throws IOException
     */
    public void saveWaveFiles(String waveFileName) throws IOException {

       saveWave(waveFileName + "_rec", tempFileRec);
       saveWave(waveFileName + "_send", tempFileSend);

    }


    private void saveWave(String fileName, File fileToSave) throws IOException {
        int dataLength = (int) fileToSave.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(fileToSave));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            File file = new File(fileDir + fileName + ".wav");
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
     *
     * @param refine whether to refine the recorded data (trim first and last second)
     * @return double array of the latest recorded data
     */
    public double[] getRecordData(boolean refine) {

        int dataLength = (int) tempFileRec.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(tempFileRec));
            input.read(rawData);
            input.close();
        } catch (IOException e){
            //
        }

        if(refine)
            return refineData(convertToDoubles(converToShorts(rawData)));
        else
            return convertToDoubles(converToShorts(rawData));
    }

    /**
     * converts the sent byte stream into double array
     * @return double array of the latest sent data
     */
    public double[] getSentData() {

        int dataLength = (int) tempFileSend.length();
        byte[] rawData = new byte[dataLength];

        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(tempFileSend));
            input.read(rawData);
            input.close();
        } catch (IOException e){
            //
        }

        double[] data = convertToDoubles(converToShorts(rawData));
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
        return tempFileRec != null && tempFileRec.length() > 0;
    }

    public void setSignalGenerator(SignalGenerator signalGenerator) {
        this.signalGen = signalGenerator;
    }
}
