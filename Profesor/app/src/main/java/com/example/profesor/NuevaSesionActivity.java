package com.example.profesor;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.profesor.data.SesionRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseError;

/** P-PF02: Crear sesión de asistencia (CU-PF02, RF-03, RF-04, RF-05, RF-17). */
public class NuevaSesionActivity extends AppCompatActivity {

    private static final int CURSO_MIN = 3;
    private static final int CURSO_MAX = 60;

    private final SesionRepository repository = new SesionRepository();

    private TextInputLayout tilCurso;
    private TextInputEditText etCurso;
    private MaterialButton btnCrear;
    private MaterialButton btnCancelar;
    private ProgressBar progressCrear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nueva_sesion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        tilCurso = findViewById(R.id.tilCurso);
        etCurso = findViewById(R.id.etCurso);
        btnCrear = findViewById(R.id.btnCrear);
        btnCancelar = findViewById(R.id.btnCancelar);
        progressCrear = findViewById(R.id.progressCrear);

        btnCrear.setOnClickListener(v -> crearSesion());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void crearSesion() {
        String curso = etCurso.getText().toString().trim();

        if (curso.length() < CURSO_MIN || curso.length() > CURSO_MAX) {
            tilCurso.setError(getString(R.string.error_curso));
            return;
        }
        tilCurso.setError(null);

        mostrarCargando(true);

        repository.crearSesion(curso, new SesionRepository.CrearSesionCallback() {
            @Override
            public void onCreada(String idSesion, String codigo, String curso) {
                mostrarCargando(false);
                Toast.makeText(NuevaSesionActivity.this,
                        getString(R.string.mensaje_sesion_creada, codigo),
                        Toast.LENGTH_LONG).show();
                abrirSesionActiva(idSesion, codigo, curso);
            }

            @Override
            public void onError(@NonNull DatabaseError error) {
                mostrarCargando(false);
                Toast.makeText(NuevaSesionActivity.this,
                        SesionRepository.obtenerMensajeError(error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirSesionActiva(String idSesion, String codigo, String curso) {
        finish();
        startActivity(SesionActivaActivity.crearIntent(this, idSesion, codigo, curso));
    }

    private void mostrarCargando(boolean cargando) {
        btnCrear.setEnabled(!cargando);
        btnCancelar.setEnabled(!cargando);
        progressCrear.setVisibility(cargando ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }
}