package com.example.profesor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.profesor.data.SesionRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** P-PF03: Sesión activa con código, contador y listado en tiempo real (CU-PF03, CU-PF04). */
public class SesionActivaActivity extends AppCompatActivity {

    public static final String EXTRA_ID_SESION = "extra_id_sesion";
    public static final String EXTRA_CODIGO = "extra_codigo";
    public static final String EXTRA_CURSO = "extra_curso";

    private final SesionRepository repository = new SesionRepository();
    private final List<AlumnoAsistente> alumnos = new ArrayList<>();
    private final AlumnosAdapter adapter = new AlumnosAdapter();

    private String idSesion;
    private TextView tvCodigo;
    private TextView tvCurso;
    private TextView tvContador;
    private TextView tvSinAlumnos;
    private RecyclerView rvAlumnos;
    private MaterialButton btnCerrarSesion;
    private ProgressBar progressCerrar;
    private boolean sesionCerrada;

    private final ValueEventListener alumnosListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // RF-07, RF-08, RF-16: snapshot con todos los alumnos ordenable por hora.
            TreeMap<String, AlumnoAsistente> ordenados = new TreeMap<>();
            for (DataSnapshot hijo : snapshot.getChildren()) {
                String nombre = hijo.child("nombre").getValue(String.class);
                String hora = hijo.child("horaRegistro").getValue(String.class);
                String clave = hora != null ? hora : hijo.getKey();
                ordenados.put(clave, new AlumnoAsistente(hijo.getKey(),
                        nombre != null ? nombre : "", hora != null ? hora : ""));
            }
            alumnos.clear();
            alumnos.addAll(ordenados.values());
            adapter.actualizarLista(alumnos);
            actualizarContador();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(SesionActivaActivity.this,
                    SesionRepository.obtenerMensajeError(error), Toast.LENGTH_LONG).show();
        }
    };

    static Intent crearIntent(Context contexto, String idSesion, String codigo, String curso) {
        Intent intent = new Intent(contexto, SesionActivaActivity.class);
        intent.putExtra(EXTRA_ID_SESION, idSesion);
        intent.putExtra(EXTRA_CODIGO, codigo);
        intent.putExtra(EXTRA_CURSO, curso);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sesion_activa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        idSesion = getIntent().getStringExtra(EXTRA_ID_SESION);
        String codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        String curso = getIntent().getStringExtra(EXTRA_CURSO);

        tvCodigo = findViewById(R.id.tvCodigo);
        tvCurso = findViewById(R.id.tvCurso);
        tvContador = findViewById(R.id.tvContador);
        tvSinAlumnos = findViewById(R.id.tvSinAlumnos);
        rvAlumnos = findViewById(R.id.rvAlumnos);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        progressCerrar = findViewById(R.id.progressCerrar);

        tvCodigo.setText(codigo != null ? codigo : "");
        tvCurso.setText(curso != null ? curso : "");

        rvAlumnos.setLayoutManager(new LinearLayoutManager(this));
        rvAlumnos.setAdapter(adapter);

        repository.escucharAlumnos(idSesion, alumnosListener);

        btnCerrarSesion.setOnClickListener(v -> confirmarCierre());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // RNF-02: remover listener al destruir la Activity.
        repository.removerListener(idSesion, alumnosListener);
    }

    private void actualizarContador() {
        int total = adapter.getTotal();
        tvContador.setText(String.valueOf(total));
        tvSinAlumnos.setVisibility(total == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void confirmarCierre() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_cerrar_titulo)
                .setMessage(R.string.dialog_cerrar_mensaje)
                .setPositiveButton(R.string.btn_confirmar, (d, w) -> cerrarSesion())
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void cerrarSesion() {
        mostrarCargando(true);

        repository.cerrarSesion(idSesion, new SesionRepository.CerrarSesionCallback() {
            @Override
            public void onCerrada() {
                mostrarCargando(false);
                sesionCerrada = true;
                btnCerrarSesion.setEnabled(false);
                btnCerrarSesion.setText(getString(R.string.mensaje_sesion_cerrada));
                Toast.makeText(SesionActivaActivity.this,
                        R.string.mensaje_sesion_cerrada, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull DatabaseError error) {
                mostrarCargando(false);
                Toast.makeText(SesionActivaActivity.this,
                        SesionRepository.obtenerMensajeError(error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarCargando(boolean cargando) {
        btnCerrarSesion.setEnabled(!cargando && !sesionCerrada);
        progressCerrar.setVisibility(cargando ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }
}