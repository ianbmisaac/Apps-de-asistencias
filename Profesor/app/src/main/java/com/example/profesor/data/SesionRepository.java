package com.example.profesor.data;

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
import java.util.Random;

/**
 * Capa repository (RNF-03): encapsula toda la interacción con Firebase Realtime Database.
 * Contrato compartido con la App Alumno (Caso 3):
 *   sesiones/{idSesion}: { activa, codigo, curso, creadoEn, alumnos/{idAlumno}: {nombre, horaRegistro} }
 */
public class SesionRepository {

    private static final String RUTA_SESIONES = "sesiones";

    /** Límite de intentos al generar un idSesion/codigo único (RF-04). */
    private static final int MAX_INTENTOS = 12;

    private final Random aleatorio = new Random();
    private final DatabaseReference sesionesRef;

    public interface CrearSesionCallback {
        void onCreada(String idSesion, String codigo, String curso);
        void onError(@NonNull DatabaseError error);
    }

    public interface CerrarSesionCallback {
        void onCerrada();
        void onError(@NonNull DatabaseError error);
    }

    public SesionRepository() {
        sesionesRef = FirebaseDatabase.getInstance().getReference(RUTA_SESIONES);
    }

    /**
     * Crea una sesión (RF-03, RF-05): escribe un nodo en sesiones/{idSesion}
     * con curso (3-60 chars) y activa: true. Genera id y código de 4 dígitos
     * únicos (RF-04) verificando antes de escribir.
     */
    public void crearSesion(String curso, CrearSesionCallback callback) {
        intentarCrearSesion(generarIdSesion(), generarCodigo(), curso, callback, 0);
    }

    @SuppressWarnings("unused")
    private void intentarCrearSesion(String idSesion, String codigo, String curso,
                                     CrearSesionCallback callback, int intento) {
        if (intento >= MAX_INTENTOS) {
            callback.onError(DatabaseError.fromCode(DatabaseError.OPERATION_FAILED));
            return;
        }

        sesionesRef.child(idSesion).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot nodoId) {
                if (nodoId.exists()) {
                    intentarCrearSesion(generarIdSesion(), generarCodigo(), curso, callback, intento + 1);
                    return;
                }
                confirmarCodigoUnico(idSesion, codigo, curso, callback, intento);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error);
            }
        });
    }

    private void confirmarCodigoUnico(String idSesion, String codigo, String curso,
                                      CrearSesionCallback callback, int intento) {
        // RNF-05: búsqueda por código indexada (orderByChild + equalTo).
        sesionesRef.orderByChild("codigo").equalTo(codigo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            intentarCrearSesion(generarIdSesion(), generarCodigo(), curso, callback, intento + 1);
                            return;
                        }
                        escribirSesion(idSesion, codigo, curso, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error);
                    }
                });
    }

    private void escribirSesion(String idSesion, String codigo, String curso,
                                CrearSesionCallback callback) {
        Map<String, Object> sesion = new HashMap<>();
        sesion.put("activa", true);
        sesion.put("codigo", codigo);
        sesion.put("curso", curso);
        sesion.put("creadoEn", Instant.now().toString());

        sesionesRef.child(idSesion).setValue(sesion, (error, ref) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onCreada(idSesion, codigo, curso);
        });
    }

    /** Listener en sesiones/{id}/alumnos (RF-07, RF-16, RNF-02: el caller remueve el listener). */
    public void escucharAlumnos(String idSesion, ValueEventListener listener) {
        sesionesRef.child(idSesion).child("alumnos")
                .addValueEventListener(listener);
    }

    public void removerListener(String idSesion, ValueEventListener listener) {
        sesionesRef.child(idSesion).child("alumnos")
                .removeEventListener(listener);
    }

    /** Cierra la sesión (RF-09): activa: false. Bloquea nuevos registros en la app Alumno. */
    public void cerrarSesion(String idSesion, CerrarSesionCallback callback) {
        sesionesRef.child(idSesion).child("activa").setValue(false, (error, ref) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            callback.onCerrada();
        });
    }

    /** Id formato SES-YYYYMMDD-NNN (contrato de datos). */
    private String generarIdSesion() {
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dia = Instant.now().atZone(ZoneId.systemDefault()).format(fecha);
        int n = 100 + aleatorio.nextInt(900);
        return String.format("SES-%s-%03d", dia, n);
    }

    /** Código numérico de 4 dígitos entre 1000 y 9999 (RF-04). */
    private String generarCodigo() {
        return Integer.toString(1000 + aleatorio.nextInt(9000));
    }

    /** Mapea un DatabaseError a un mensaje legible en español (ERR-04). */
    public static String obtenerMensajeError(DatabaseError error) {
        if (error != null && error.getCode() == DatabaseError.UNAVAILABLE) {
            return "Sin conexión";
        }
        return "No se pudo completar la operación";
    }

    /** Formatea un horario ISO 8601 a hora local HH:mm:ss. */
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