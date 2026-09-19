package com.example.profesor;

/** Modelo de un alumno registrado en la sesión (item del listado P-PF03). */
public class AlumnoAsistente {

    private String id;
    private String nombre;
    private String horaRegistro;

    public AlumnoAsistente() {
    }

    public AlumnoAsistente(String id, String nombre, String horaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.horaRegistro = horaRegistro;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHoraRegistro() {
        return horaRegistro;
    }
}