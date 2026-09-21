package com.example.alumnos;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alumnos.data.SesionRepository;
import com.google.android.material.button.MaterialButton;

public class ConfirmacionActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO = "extra_curso";
    public static final String EXTRA_CODIGO = "extra_codigo";
    public static final String EXTRA_HORA = "extra_hora";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmacion);

        findViewById(R.id.root).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_in_up));

        reproducirSonidoExito();
        vibrarExito();

        String curso = getIntent().getStringExtra(EXTRA_CURSO);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        String hora = getIntent().getStringExtra(EXTRA_HORA);

        TextView tvCurso = findViewById(R.id.tvCurso);
        TextView tvCodigo = findViewById(R.id.tvCodigo);
        TextView tvHora = findViewById(R.id.tvHora);

        tvCurso.setText(curso != null ? curso : "");
        tvCodigo.setText(codigo != null ? codigo : "");
        tvHora.setText(SesionRepository.formatearFechaLegible(hora));

        MaterialButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());
    }

    /** Feedback háptico breve cuando la asistencia queda registrada. */
    private void vibrarExito() {
        Vibrator vibrador = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createOneShot(120,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    /** Sonido breve de confirmación cuando la asistencia queda registrada. */
    private void reproducirSonidoExito() {
        Ringtone tono = RingtoneManager.getRingtone(this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (tono != null) {
            tono.play();
        }
    }
}