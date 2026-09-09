package com.example.alumnos;

import android.os.Bundle;
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

        String curso = getIntent().getStringExtra(EXTRA_CURSO);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        String hora = getIntent().getStringExtra(EXTRA_HORA);

        TextView tvCurso = findViewById(R.id.tvCurso);
        TextView tvCodigo = findViewById(R.id.tvCodigo);
        TextView tvHora = findViewById(R.id.tvHora);

        tvCurso.setText(curso != null ? curso : "");
        tvCodigo.setText(codigo != null ? codigo : "");
        tvHora.setText(SesionRepository.formatearHora(hora));

        MaterialButton btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());
    }
}