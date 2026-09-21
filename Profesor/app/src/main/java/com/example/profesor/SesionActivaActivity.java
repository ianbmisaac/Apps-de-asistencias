package com.example.profesor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private TextView tvIniciada;
    private RecyclerView rvAlumnos;
    private MaterialButton btnCerrarSesion;
    private ProgressBar progressCerrar;
    private boolean sesionCerrada;

    private final Set<String> clavesConocidas = new HashSet<>();
    private boolean primeraCarga = true;

    private final ValueEventListener alumnosListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // RF-07, RF-08, RF-16: snapshot con todos los alumnos ordenado por hora de llegada.
            TreeMap<String, AlumnoAsistente> ordenados = new TreeMap<>();
            List<String> nuevos = new ArrayList<>();
            for (DataSnapshot hijo : snapshot.getChildren()) {
                String nombre = hijo.child("nombre").getValue(String.class);
                String hora = hijo.child("horaRegistro").getValue(String.class);
                // Clave única (hora + id) para que dos alumnos con la misma hora no se pisen.
                ordenados.put((hora != null ? hora : hijo.getKey()) + "|" + hijo.getKey(),
                        new AlumnoAsistente(hijo.getKey(),
                                nombre != null ? nombre : "", hora != null ? hora : ""));
                if (!primeraCarga && !clavesConocidas.contains(hijo.getKey())) {
                    nuevos.add(nombre != null ? nombre : hijo.getKey());
                }
            }
            alumnos.clear();
            alumnos.addAll(ordenados.values());
            adapter.actualizarLista(alumnos);
            actualizarContador();

            if (!primeraCarga) {
                for (String nuevo : nuevos) {
                    notificarNuevoAlumno(nuevo);
                }
            }
            primeraCarga = false;
            clavesConocidas.clear();
            for (DataSnapshot hijo : snapshot.getChildren()) {
                clavesConocidas.add(hijo.getKey());
            }
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

        findViewById(R.id.root).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.fade_in_up));

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
        tvIniciada = findViewById(R.id.tvIniciada);
        rvAlumnos = findViewById(R.id.rvAlumnos);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        progressCerrar = findViewById(R.id.progressCerrar);

        tvCodigo.setText(codigo != null ? codigo : "");
        tvCurso.setText(curso != null ? curso : "");

        MaterialButton btnCopiar = findViewById(R.id.btnCopiar);
        btnCopiar.setOnClickListener(v -> copiarCodigo());

        adapter.setOnLongClickAlumno(this::confirmarQuitarAlumno);

        rvAlumnos.setLayoutManager(new LinearLayoutManager(this));
        rvAlumnos.setAdapter(adapter);

        repository.escucharAlumnos(idSesion, alumnosListener);
        repository.leerCreadoEn(idSesion, creadoEn -> {
            if (creadoEn != null && !creadoEn.isEmpty()) {
                tvIniciada.setText(getString(R.string.label_iniciada,
                        SesionRepository.formatearFechaLegible(creadoEn)));
            }
        });

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

    private void copiarCodigo() {
        ClipboardManager portapapeles =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        portapapeles.setPrimaryClip(ClipData.newPlainText("codigo_sesion", tvCodigo.getText()));
        Toast.makeText(this, R.string.toast_codigo_copiado, Toast.LENGTH_SHORT).show();
    }

    private void notificarNuevoAlumno(String nombre) {
        Ringtone tono = RingtoneManager.getRingtone(this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (tono != null) {
            tono.play();
        }
        Toast.makeText(this, getString(R.string.mensaje_nuevo_alumno, nombre),
                Toast.LENGTH_SHORT).show();
        rvAlumnos.smoothScrollToPosition(0);
        rvAlumnos.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
                .withEndAction(() -> rvAlumnos.animate().scaleX(1f).scaleY(1f)
                        .setDuration(100).start())
                .start();
    }

    private void confirmarQuitarAlumno(AlumnoAsistente alumno) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_quitar_titulo)
                .setMessage(getString(R.string.dialog_quitar_mensaje, alumno.getNombre()))
                .setPositiveButton(R.string.btn_quitar, (d, w) -> quitarAlumno(alumno))
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void quitarAlumno(AlumnoAsistente alumno) {
        repository.eliminarAlumno(idSesion, alumno.getId(),
                new SesionRepository.EliminarAlumnoCallback() {
                    @Override
                    public void onEliminado() {
                        Toast.makeText(SesionActivaActivity.this,
                                R.string.toast_alumno_eliminado, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull DatabaseError error) {
                        Toast.makeText(SesionActivaActivity.this,
                                SesionRepository.obtenerMensajeError(error),
                                Toast.LENGTH_LONG).show();
                    }
                });
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
                Toast.makeText(SesionActivaActivity.this,
                        R.string.mensaje_sesion_cerrada, Toast.LENGTH_LONG).show();
                // Vuelve al panel del profesor (P-PF01).
                finish();
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