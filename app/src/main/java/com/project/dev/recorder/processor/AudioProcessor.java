/*
 * @fileoverview    {AudioProcessor}
 *
 * @version         2.0
 *
 * @author          Dyson Arley Parra Tilano <dysontilano@gmail.com>
 *
 * @copyright       Dyson Parra
 * @see             github.com/DysonParra
 *
 * History
 * @version 1.0     Implementation done.
 * @version 2.0     Documentation added.
 */
package com.project.dev.recorder.processor;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * TODO: Description of {@code AudioProcessor}.
 *
 * @author Dyson Parra
 * @since Java 17 (LTS), Gradle 7.3
 */
public class AudioProcessor implements Runnable {

    private ByteArrayOutputStream os = new ByteArrayOutputStream();         // Crea objeto de tipo ByteArrayOutputStream.
    private PrintStream printer = new PrintStream(os);                      // Crea objeto de tipo PrintStream
    private String output;                                                  // Crea String.

    private static final String TAG = AudioProcessor.class.getCanonicalName();

    /**
     * TODO: Description of {@code PitchDetectionListener}.
     *
     */
    public interface PitchDetectionListener {

        /**
         *
         * @param freq
         * @param avgIntensity
         */
        public abstract void onPitchDetected(float freq, double avgIntensity);
    }

    private AudioRecord mAudioRecord;
    private PitchDetectionListener mPitchDetectionListener;
    private boolean mStop = false;

    /**
     * TODO: Description of {@code setPitchDetectionListener}.
     *
     * @param pitchDetectionListener
     */
    public void setPitchDetectionListener(PitchDetectionListener pitchDetectionListener) {
        mPitchDetectionListener = pitchDetectionListener;
        Log.d("printList", String.valueOf("Pasa 11 setPitchDetectionListener"));
    }

    /**
     * FIXME: Description of {@code init}. Inicializa el grabador de audio.
     *
     */
    public void init() {
        Log.d("printList", String.valueOf("Pasa 12 init"));

        int[] SAMPLE_RATES = {44100, 22050, 16000, 11025, 8000};                                    // Matriz con todas la frecuencias de muestreo posibles para grabar.
        int bufSize = 16384;                                                                        // Crea bufSize que tendrá el tamaño mínimo (tentativo) del buffer de audio.
        int i = 0;                                                                                  // Indica el número de frecuencia de muestreo actual.

        // Recorre la matriz con las frecuencias de muestreo.
        do {
            int sampleRate = SAMPLE_RATES[i];                                                       //A sampleRate le lleva la frecuencia de muestreo actual en la matriz.
            int minBufSize = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);                   // A minBufSize le lleva el tamaño minimo del buffer con la coonfiguración actual.

            if (minBufSize != AudioRecord.ERROR_BAD_VALUE && minBufSize != AudioRecord.ERROR) {     // Si con el tamaño actual de bufer no se produce un error al intentar grabar.
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(bufSize, minBufSize * 4));                                         // Inicializa el grabador de audio.

                //Log.d("printList", "Buffer: " + String.valueOf( minBufSize * 4));
            }

            i++;                                                                                    // Pasa a la siguiente posición del array de frecuencias de muestreo.
        } // Si el grabador de audio se creó exitosamente no intenta con las demás frecuencias de muestreo.
        while (i < SAMPLE_RATES.length && (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED));
    }

    /**
     * FIXME: Description of {@code run}. Empieza a grabar audio (sobreescribe el funcionamiento ya
     * que implementa Runnable o thread).
     *
     */
    @Override
    public void run() {
        Log.d("printList", String.valueOf("Pasa 13 run"));

        mAudioRecord.startRecording();
        int bufSize = 8192;
        final int sampleRate = mAudioRecord.getSampleRate();
        final short[] buffer = new short[bufSize];

        do {
            //Log.d("printList", String.valueOf("Pasa 15-16"));
            final int read = mAudioRecord.read(buffer, 0, bufSize);

            if (read > 0) {
                // Promedio del valor absoluto de cada uno de los 8192 frames.
                final double intensity = averageIntensity(buffer, read);

                // zeroCrossingCount(buffer) = Cantidad de frames con cambios entre positivo y negativo.
                int maxZeroCrossing = (int) (500 * (read / bufSize) * (sampleRate / 44100.0));

                // Non static block.
                {
                    //Log.d("printList", String.valueOf("Pasa 13.5 run"));

                    // A♭0 = 25,956 C#8 = 4434.921.
                    float freq = getPitch(buffer, read / 4, read, sampleRate, 26, 4435);

                    printer.printf("%3d %4d %8.3f %8.3f", maxZeroCrossing, zeroCrossingCount(buffer), intensity, freq);
                    output = os.toString();                         // Asigna a output el buffer os.
                    Log.d("printList", output);                // Muestra en consola output.
                    os = new ByteArrayOutputStream();               // Asigna un nuevo buffer a os.
                    printer = new PrintStream(os);                  // Realaciona printer con os.

                    if (zeroCrossingCount(buffer) <= maxZeroCrossing && freq < 4435)
                        mPitchDetectionListener.onPitchDetected(freq, intensity);
                }
            }
        } while (!mStop);

        Log.d(TAG, "Thread terminated");
    }

    /**
     * FIXME: Description of {@code stop}. Detiene la grabación de audio.
     *
     */
    public void stop() {
        Log.d("printList", String.valueOf("Pasa 14 stop"));
        mStop = true;
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    /**
     * FIXME: Description of {@code averageIntensity}. Obtiene el volumen promedio de las muestras
     * leídas.
     *
     * @param data   Es la matriz con las muestras.
     * @param frames Es la cantidad de muestras.
     * @return
     */
    private double averageIntensity(short[] data, int frames) {
        double sum = 0;                                                     // Indica la suma del valor de todas las muestras.
        for (int i = 0; i < frames; i++)                                    // Recorre la matriz con las muestras.
            sum += Math.abs(data[i]);                                       // Obtiene sum lo que tiene más el valor absoluto de la muestra actual.

        return sum / frames;                                                // Devuelve el valor de sum dividido entre la cantidad de muestras.
    }

    /**
     * FIXME: Description of {@code zeroCrossingCount}. Obtiene la cantidad de saltos entre positivo
     * y negativo en las muestras leídas.
     *
     * @param data Es la matriz con las muestras.
     * @return
     */
    private int zeroCrossingCount(short[] data) {
        int len = data.length;                                              // Cantidad de muestras.
        int count = 0;                                                      // Número de cambios entre positivo y negativo.
        boolean prevValPositive;                                            // Si la muestra anterior tiene un valor positivo.
        boolean positive;                                                   // Si la muestra actual tiene un valor positivo.

        prevValPositive = data[0] >= 0;                                     // A prevValPositive le lleva si la primera muestra tiene un valor positivo.

        for (int i = 1; i < len; i++) {                                     // Recorre la matriz con las muestras desde la segunda posición.
            positive = data[i] >= 0;                                        // A positive le lleva si la muestra catual es positiva.
            if (prevValPositive == !positive)                               // Si la muestra anterior tiene un signo diferente a la actual.
                count++;                                                    // Aumenta count.
            prevValPositive = positive;                                     // A la prevValPositive le lleva positive.
        }

        return count;                                                       // Devuelve la cantiad de cambios entre positivo y negativo.
    }

    /**
     * FIXME: Description of {@code getPitch}. Obtiene la frecuencia (en Hertz) de las muestras
     * leídas.
     *
     * @param data       Es la matriz con las muestras.
     * @param windowSize Es el tamaño de la ventana.
     * @param frames     Es la cantidad de muestras.
     * @param sampleRate Es la frecuencia de muestreo del buffer.
     * @param minFreq    Es el valor mínimo de frecuencia para que no sea ignorada.
     * @param maxFreq    Es el valor máximo de frecuencia para que no sea ignorada.
     * @return
     */
    private float getPitch(short[] data, int windowSize, int frames, float sampleRate, float minFreq, float maxFreq) {
        float maxOffset = sampleRate / minFreq;             //882
        float minOffset = sampleRate / maxFreq;             //88

        int minSum = Integer.MAX_VALUE;                     //2.147.483.647
        int minSumLag = 0;
        int[] sums = new int[Math.round(maxOffset) + 2];    //884 positions

        for (int lag = (int) minOffset; lag <= maxOffset; lag++) {
            int sum = 0;
            for (int i = 0; i < windowSize; i++) {
                int oldIndex = i - lag;
                int sample = ((oldIndex < 0) ? data[frames + oldIndex] : data[oldIndex]);
                sum += Math.abs(sample - data[i]);
            }

            sums[lag] = sum;

            if (sum < minSum) {
                minSum = sum;
                minSumLag = lag;
            }
        }

        // quadratic interpolation
        float delta = (sums[minSumLag + 1] - sums[minSumLag - 1]) / ((float) (2 * (2 * sums[minSumLag] - sums[minSumLag + 1] - sums[minSumLag - 1])));

        return sampleRate / (minSumLag + delta);
    }
}
