package jakobkarolus.de.ultrasense.audio;

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

import jakobkarolus.de.ultrasense.features.FeatureDetector;

/**
 * interacts with the android audio system during recording and detection.
 *
 * <br><br>
 * Created by Jakob on 25.05.2015.
 */
public class AudioManager{

    private Context ctx;
    private static final String fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + "UltraSense" + File.separator;

    private static final int SAMPLE_RATE = 44100;
    private DataOutputStream dosSend;
    private DataOutputStream dosRec;
    private File tempFileRec;
    private File tempFileSend;

    private SignalGenerator signalGen;

    private boolean recordRunning = false;
    private boolean detectionRunning = false;
    private FeatureDetector featureDetector;

    //force a multiple of 4096
    private static final int minSize = 4*4096;//AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);


    public AudioManager(Context ctx){
        this.ctx = ctx;
        new File(fileDir).mkdirs();
    }


    /**
     * starts detecting features/gesture depending on the used extractors.<br>
     * Does not record the data for later analysis
     *
     */
    public void startDetection(){

        final AudioTrack at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
        final AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*minSize);

        final byte[] audio =  signalGen.generateAudio();

        Thread detectionThread = new Thread(new Runnable() {

            private int sampleCounter;
            @Override
            public void run() {

                final byte[] buffer = new byte[minSize];
                while(detectionRunning){
                    int samplesRead = ar.read(buffer, 0, minSize);
                    if(samplesRead != minSize) {
                        Log.e("AUDIO_MANAGER", "Samples read not equal minSize (" + samplesRead + "). Might be loosing data!");
                    }

                    //size of bufferDouble is buffer.length/2
                    final double[] bufferDouble = convertToDouble(buffer, samplesRead);

                    if(sampleCounter >= 44100) {
                        if(featureDetector != null) {
                            featureDetector.checkForFeatures(bufferDouble, true);
                        }
                    }
                    //omit first second
                    sampleCounter+=samplesRead;
                }

                ar.stop();
                ar.release();
            }
        });

        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {

                at.play();
                while(detectionRunning){
                    at.write(audio, 0, audio.length);
                }
                at.stop();
                at.release();
            }
        });

        ar.startRecording();
        detectionRunning = true;
        detectionThread.start();
        playThread.start();
    }


    /**
     * stops feature/gesture detection
     */
    public void stopDetection() {
        detectionRunning = false;
    }

    /**
     * starts recording the received audio. Also saves it to a file for later analysis
     *
     * @throws FileNotFoundException
     */
    public void startRecord() throws FileNotFoundException {

        final AudioTrack at = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,minSize,AudioTrack.MODE_STREAM);
        final AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*minSize);

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

                final byte[] buffer = new byte[minSize];
                while(recordRunning){
                    int samplesRead = ar.read(buffer, 0, minSize);
                    if(samplesRead != minSize) {
                        Log.e("AUDIO_MANAGER", "Samples read not equal minSize (" + samplesRead + "). Might be loosing data!");
                    }

                    //TODO: consider seperate thread (in case of buffer overflow)
                    writeByteBufferToStream(buffer, dosRec);

                    //size of bufferDouble is buffer.length/2
                    final double[] bufferDouble = convertToDouble(buffer, samplesRead);

                    if(sampleCounter >= 44100) {
                        if(featureDetector != null) {
                            featureDetector.checkForFeatures(bufferDouble, true);
                        }

                    }

                    //omit first second
                    sampleCounter+=samplesRead;
                }

                ar.stop();
                ar.release();
                try {
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
                at.stop();
                at.release();
            }
        });

        ar.startRecording();
        recordRunning = true;
        recordThread.start();
        playThread.start();

    }

    /**
     * Finishes play and record thread and ask for filename.<br>
     * Saved files can be found in the app directory (in /Downloads)
     *
     * @throws IOException
     */
    public void stopRecord() throws IOException {
        recordRunning = false;
    }

    private double[] convertToDouble(byte[] buffer, int bytesRead) {

        //from http://stackoverflow.com/questions/5774104/android-audio-fft-to-retrieve-specific-frequency-magnitude-using-audiorecord
        double[] bufferDouble = new double[buffer.length/2];
        final int bytesPerSample = 2; // As it is 16bit PCM
        final double amplification = 1.0; // choose a number as you like
        for (int index = 0, floatIndex = 0; index < bytesRead - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = buffer[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            bufferDouble[floatIndex] = sample32;
        }

        return bufferDouble;
    }


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
            dos.flush();

        } catch(IOException e){
            e.printStackTrace();
        }
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
     * converts the recorded byte stream into double array
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


    public boolean hasRecordData() {
        return tempFileRec != null && tempFileRec.length() > 0;
    }

    public void setSignalGenerator(SignalGenerator signalGenerator) {
        this.signalGen = signalGenerator;
    }

    public void setFeatureDetector(FeatureDetector featureDetector){
        this.featureDetector = featureDetector;
    }
}
