package com.example.profesor.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
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

    public interface EliminarAlumnoCallback {
        void onEliminado();
        void onError(@NonNull DatabaseError error);
    }

    public interface CreadoEnCallback {
        void onLeido(String creadoEn);
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

    /** Elimina un alumno del registro (solo si el nodo existe; la regla RTDB lo permite). */
    public void eliminarAlumno(String idSesion, String idAlumno, EliminarAlumnoCallback callback) {
        sesionesRef.child(idSesion).child("alumnos").child(idAlumno)
                .setValue(null, (error, ref) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    callback.onEliminado();
                });
    }

    /** Lee la fecha de creación de la sesión (creadoEn). */
    public void leerCreadoEn(String idSesion, CreadoEnCallback callback) {
        sesionesRef.child(idSesion).child("creadoEn")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object valor = snapshot.getValue();
                        callback.onLeido(valor != null ? valor.toString() : "");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onLeido("");
                    }
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

    /** Formatea un horario a hora local legible. Soporta formato local (nuevo) y UTC (legacy). */
    public static String formatearHora(String horaIso) {
        if (horaIso == null || horaIso.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime fechaHora;
            try {
                fechaHora = LocalDateTime.parse(horaIso);
            } catch (Exception ignorada) {
                fechaHora = Instant.parse(horaIso)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            return fechaHora.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return horaIso;
        }
    }

    /** Hora corta, o "d 'de' MMM · HH:mm" si el registro es de otro día (bonificación). */
    public static String formatearHoraConFecha(String horaIso) {
        if (horaIso == null || horaIso.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime fechaHora;
            try {
                fechaHora = LocalDateTime.parse(horaIso);
            } catch (Exception ignorada) {
                fechaHora = Instant.parse(horaIso)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
            String hora = fechaHora.format(DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate dia = fechaHora.toLocalDate();
            if (dia.equals(LocalDate.now())) {
                return hora;
            }
            String fecha = dia.getYear() == LocalDate.now().getYear()
                    ? dia.format(DateTimeFormatter.ofPattern("d 'de' MMM", Locale.getDefault()))
                    : dia.format(DateTimeFormatter.ofPattern("d 'de' MMM yyyy", Locale.getDefault()));
            return fecha + " · " + hora;
        } catch (Exception e) {
            return horaIso;
        }
    }

    /** Formatea una fecha a una forma amigable: "hoy, 20:41", "ayer, 20:41", "20 sep, 20:41".
     * Soporta ISO local (nuevo), ISO UTC (legacy) y epoch millis (ServerValue.TIMESTAMP). */
    public static String formatearFechaLegible(String fecha) {
        if (fecha == null || fecha.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime momento;
            try {
                momento = LocalDateTime.parse(fecha);
            } catch (Exception e1) {
                try {
                    momento = Instant.parse(fecha)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                } catch (Exception e2) {
                    long millis = Long.parseLong(fecha);
                    momento = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }
            LocalDate hoy = LocalDate.now();
            LocalDate dia = momento.toLocalDate();
            String cuando;
            if (dia.equals(hoy)) {
                cuando = "hoy";
            } else if (dia.equals(hoy.minusDays(1))) {
                cuando = "ayer";
            } else if (dia.getYear() == hoy.getYear()) {
                cuando = dia.format(
                        DateTimeFormatter.ofPattern("d 'de' MMM", Locale.getDefault()));
            } else {
                cuando = dia.format(
                        DateTimeFormatter.ofPattern("d 'de' MMM yyyy", Locale.getDefault()));
            }
            String hora = momento.format(DateTimeFormatter.ofPattern("HH:mm"));
            return cuando + ", " + hora;
        } catch (Exception e) {
            return fecha;
        }
    }
}