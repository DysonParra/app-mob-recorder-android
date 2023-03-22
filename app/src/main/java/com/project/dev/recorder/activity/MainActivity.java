/*
 * @fileoverview    {MainActivity}
 *
 * @version         2.0
 *
 * @author          Dyson Arley Parra Tilano <dysontilano@gmail.com>
 *
 * @copyright       Dyson Parra
 * @see             github.com/DysonParra
 *
 * History
 * @version 1.0     Implementación realizada.
 * @version 2.0     Documentación agregada.
 */
package com.project.dev.recorder.activity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.project.dev.recorder.R;
import com.project.dev.recorder.processor.AudioProcessor;

/**
 * TODO: Definición de {@code MainActivity}.
 *
 * @author Dyson Parra
 * @since 1.8
 */
public class MainActivity extends AppCompatActivity {

    /*
     * Variables asociadas con elementos la vista.
     */
    private Button btnRecord;
    private Button btnFft;
    private TextView txtSource;
    private TextView txtHz;

    /*
     * Variables locales.
     */
    private AudioProcessor mAudioProcessor;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private boolean recording = false;

    /**
     * Invocado cuando se crea el activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Crea instancia del activity y la asocia con la vista.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Asocia variables locales con elementos de la vista.
        btnRecord = findViewById(R.id.btnRecord);
        btnFft = findViewById(R.id.btnFft);
        txtSource = findViewById(R.id.txtSource);
        txtHz = findViewById(R.id.txtHz);
    }

    /**
     * TODO: Definición de {@code onStart}.
     *
     */
    @Override
    protected void onStart() {
        super.onStart();
        //startAudioProcessing();

        // Comportamiento del botón agregar sonido.
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Si está grabando.
                if (recording) {
                    // Escribe en el botón que ya no está grabando y actualiza recording.
                    btnRecord.setText(MainActivity.this.getString(R.string.btnRecord));
                    recording = false;

                    mAudioProcessor.stop();

                } // Si no está grabando (empezará a grabar).
                else {
                    // Escribe en el botón que ya está grabando y actualiza recording.
                    btnRecord.setText(MainActivity.this.getString(R.string.btnStop));
                    recording = true;

                    startAudioProcessing();

                }
            }
        });
    }

    /**
     * TODO: Definición de {@code startAudioProcessing}.
     *
     */
    private void startAudioProcessing() {
        mAudioProcessor = new AudioProcessor();
        mAudioProcessor.init();

        mAudioProcessor.setPitchDetectionListener(new AudioProcessor.PitchDetectionListener() {
            @Override
            public void onPitchDetected(final float freq, double avgIntensity) {
                Log.d("printList", String.valueOf("Pasa 07 onPitchDetected"));

                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        txtHz.setText(String.format("%.02fHz", freq));
                        Log.d("printList", String.valueOf(freq));
                    }
                });
            }
        });

        Log.d("printList", String.valueOf("Pasa 06.5 startAudioProcessing"));

        mExecutor.execute(mAudioProcessor);
    }
}
