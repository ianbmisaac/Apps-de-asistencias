package com.example.alumnos;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alumnos.data.SesionRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseError;

public class RegistroActivity extends AppCompatActivity {

    private static final int ID_MIN = 5;
    private static final int ID_MAX = 20;
    private static final int NOMBRE_MIN = 2;
    private static final int NOMBRE_MAX = 80;
    private static final int CODIGO_LEN = 4;

    private final SesionRepository repository = new SesionRepository();

    private TextInputLayout tilId;
    private TextInputLayout tilNombre;
    private TextInputLayout tilCodigo;
    private TextInputEditText etId;
    private TextInputEditText etNombre;
    private TextInputEditText etCodigo;
    private MaterialButton btnRegistrar;
    private ProgressBar progressRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        tilId = findViewById(R.id.tilId);
        tilNombre = findViewById(R.id.tilNombre);
        tilCodigo = findViewById(R.id.tilCodigo);
        etId = findViewById(R.id.etId);
        etNombre = findViewById(R.id.etNombre);
        etCodigo = findViewById(R.id.etCodigo);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        progressRegistro = findViewById(R.id.progressRegistro);

        btnRegistrar.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        if (!validarFormulario()) {
            return;
        }

        String idAlumno = etId.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String codigo = etCodigo.getText().toString().trim();

        mostrarCargando(true);

        // CU-AL03 paso 1: buscar sesión por código
        repository.buscarSesionPorCodigo(codigo, new SesionRepository.SesionCallback() {
            @Override
            public void onEncontrada(String idSesion, String curso, boolean activa) {
                if (!activa) {
                    mostrarCargando(false);
                    mostrarError(R.string.error_sesion_cerrada); // ERR-02
                    return;
                }
                registrarAsistencia(idSesion, idAlumno, nombre, codigo);
            }

            @Override
            public void onNoEncontrada() {
                mostrarCargando(false);
                mostrarError(R.string.error_sesion_no_encontrada); // ERR-01
            }

            @Override
            public void onError(DatabaseError error) {
                mostrarCargando(false);
                mostrarError(SesionRepository.obtenerMensajeError(error)); // ERR-04
            }
        });
    }

    private void registrarAsistencia(String idSesion, String idAlumno, String nombre, String codigo) {
        repository.registrarAsistencia(idSesion, idAlumno, nombre,
                new SesionRepository.RegistroCallback() {
                    @Override
                    public void onRegistrado(String idSesion, String nombre,
                                             String curso, String horaRegistro) {
                        mostrarCargando(false);
                        abrirConfirmacion(curso, codigo, horaRegistro);
                    }

                    @Override
                    public void onSesionNoExiste() {
                        mostrarCargando(false);
                        mostrarError(R.string.error_sesion_no_encontrada); // ERR-01
                    }

                    @Override
                    public void onSesionCerrada() {
                        mostrarCargando(false);
                        mostrarError(R.string.error_sesion_cerrada); // ERR-02
                    }

                    @Override
                    public void onYaRegistrado() {
                        mostrarCargando(false);
                        mostrarError(R.string.error_ya_registrado); // ERR-05
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        mostrarCargando(false);
                        mostrarError(SesionRepository.obtenerMensajeError(error)); // ERR-04
                    }
                });
    }

    /** Validación local de campos (RF-10, RF-11, ERR-03, ERR-06). */
    private boolean validarFormulario() {
        boolean valido = true;

        String id = etId.getText().toString().trim();
        if (id.length() < ID_MIN || id.length() > ID_MAX) {
            tilId.setError(getString(R.string.error_id));
            valido = false;
        } else {
            tilId.setError(null);
        }

        String nombre = etNombre.getText().toString().trim();
        if (nombre.length() < NOMBRE_MIN || nombre.length() > NOMBRE_MAX) {
            tilNombre.setError(getString(R.string.error_nombre));
            valido = false;
        } else {
            tilNombre.setError(null);
        }

        String codigo = etCodigo.getText().toString().trim();
        if (!codigo.matches("\\d{" + CODIGO_LEN + "}")) {
            tilCodigo.setError(getString(R.string.error_codigo_formato)); // ERR-06
            valido = false;
        } else {
            tilCodigo.setError(null);
        }

        return valido;
    }

    private void mostrarCargando(boolean cargando) {
        btnRegistrar.setEnabled(!cargando);
        progressRegistro.setVisibility(cargando ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    private void mostrarError(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
    }

    private void mostrarError(String mensaje) {
        if (TextUtils.isEmpty(mensaje)) {
            mensaje = getString(R.string.error_registro);
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void abrirConfirmacion(String curso, String codigo, String horaRegistro) {
        Intent intent = new Intent(this, ConfirmacionActivity.class);
        intent.putExtra(ConfirmacionActivity.EXTRA_CURSO, curso);
        intent.putExtra(ConfirmacionActivity.EXTRA_CODIGO, codigo);
        intent.putExtra(ConfirmacionActivity.EXTRA_HORA, horaRegistro);
        startActivity(intent);
    }
}