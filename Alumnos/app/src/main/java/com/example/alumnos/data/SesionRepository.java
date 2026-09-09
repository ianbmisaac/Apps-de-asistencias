package com.example.alumnos.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Capa repository (RNF-03): encapsula toda la interacción con Firebase Realtime Database.
 * Ruta base usada: sesiones/{idSesion}
 */
public class SesionRepository {

    private static final String RUTA_SESIONES = "sesiones";

    private final DatabaseReference sesionesRef;

    public SesionRepository() {
        sesionesRef = FirebaseDatabase.getInstance().getReference(RUTA_SESIONES);
    }

    /** Resultado de una búsqueda de sesión por código. */
    public interface SesionCallback {
        void onEncontrada(String idSesion, String curso, boolean activa);
        void onNoEncontrada();
        void onError(@NonNull DatabaseError error);
    }

    /** Resultado del intento de registrar asistencia. */
    public interface RegistroCallback {
        void onRegistrado(String idSesion, String nombre, String curso, String horaRegistro);
        void onSesionNoExiste();
        void onSesionCerrada();
        void onYaRegistrado();
        void onError(@NonNull DatabaseError error);
    }

    /**
     * Busca una sesión activa por su código (RF-12, RNF-05).
     * Consulta: ref("sesiones").orderByChild("codigo").equalTo(codigo)
     */
    public void buscarSesionPorCodigo(String codigo, SesionCallback callback) {
        sesionesRef.orderByChild("codigo").equalTo(codigo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            callback.onNoEncontrada();
                            return;
                        }
                        DataSnapshot nodoSesion = snapshot.getChildren().iterator().next();
                        Boolean activa = nodoSesion.child("activa").getValue(Boolean.class);
                        String curso = nodoSesion.child("curso").getValue(String.class);
                        callback.onEncontrada(
                                nodoSesion.getKey(),
                                curso != null ? curso : "",
                                activa != null && activa);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    /**
     * Registra la asistencia del alumno (RF-13).
     * Valida sesión existente, activa y no duplicado antes de escribir.
     * Escribe en: sesiones/{idSesion}/alumnos/{idAlumno}
     */
    public void registrarAsistencia(String idSesion, String idAlumno, String nombre,
                                    RegistroCallback callback) {
        DatabaseReference nodoAlumno = sesionesRef
                .child(idSesion)
                .child("alumnos")
                .child(idAlumno);

        sesionesRef.child(idSesion).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot nodoSesion) {
                if (!nodoSesion.exists()) {
                    callback.onSesionNoExiste();
                    return;
                }
                Boolean activa = nodoSesion.child("activa").getValue(Boolean.class);
                if (activa == null || !activa) {
                    callback.onSesionCerrada();
                    return;
                }
                if (nodoSesion.child("alumnos").child(idAlumno).exists()) {
                    callback.onYaRegistrado();
                    return;
                }

                String curso = nodoSesion.child("curso").getValue(String.class);
                String horaRegistro = Instant.now().toString();

                Map<String, Object> alumno = new HashMap<>();
                alumno.put("nombre", nombre);
                alumno.put("horaRegistro", horaRegistro);

                nodoAlumno.setValue(alumno, (error, ref) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    callback.onRegistrado(idSesion, nombre,
                            curso != null ? curso : "", horaRegistro);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error);
            }
        });
    }

    /** Convierte un código de error de RTDB en un mensaje legible en español (ERR-01..ERR-06). */
    public static String obtenerMensajeError(DatabaseError error) {
        switch (error.getCode()) {
            case DatabaseError.UNAVAILABLE:  // -24: sin conexión
                return "Sin conexión";
            default:
                return "No se pudo registrar la asistencia";
        }
    }

    /** Formatea un horario ISO 8601 a formato local legible. */
    public static String formatearHora(String horaIso) {
        if (horaIso == null || horaIso.isEmpty()) {
            return "";
        }
        try {
            DateTimeFormatter entrada = DateTimeFormatter.ISO_INSTANT;
            DateTimeFormatter salida = DateTimeFormatter.ofPattern("HH:mm:ss");
            return Instant.parse(horaIso).atZone(ZoneId.systemDefault()).format(salida);
        } catch (Exception e) {
            return horaIso;
        }
    }
}