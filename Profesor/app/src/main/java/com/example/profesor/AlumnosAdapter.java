package com.example.profesor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.profesor.data.SesionRepository;

import java.util.ArrayList;
import java.util.List;

/** Item del listado en tiempo real de alumnos registrados (P-PF03). */
public class AlumnosAdapter extends RecyclerView.Adapter<AlumnosAdapter.AlumnoViewHolder> {

    private final List<AlumnoAsistente> alumnos = new ArrayList<>();

    public void actualizarLista(List<AlumnoAsistente> lista) {
        alumnos.clear();
        alumnos.addAll(lista);
        notifyDataSetChanged();
    }

    public int getTotal() {
        return alumnos.size();
    }

    @NonNull
    @Override
    public AlumnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alumno, parent, false);
        return new AlumnoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull AlumnoViewHolder holder, int position) {
        AlumnoAsistente alumno = alumnos.get(position);
        holder.tvNombre.setText(alumno.getNombre());
        holder.tvId.setText(alumno.getId());
        holder.tvHora.setText(SesionRepository.formatearHora(alumno.getHoraRegistro()));
    }

    @Override
    public int getItemCount() {
        return alumnos.size();
    }

    static class AlumnoViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre;
        final TextView tvId;
        final TextView tvHora;

        AlumnoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvId = itemView.findViewById(R.id.tvId);
            tvHora = itemView.findViewById(R.id.tvHora);
        }
    }
}