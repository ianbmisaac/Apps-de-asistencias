package com.example.profesor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.profesor.data.SesionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Item del listado en tiempo real de alumnos registrados (P-PF03). */
public class AlumnosAdapter extends RecyclerView.Adapter<AlumnosAdapter.AlumnoViewHolder> {

    /** Toque largo en un ítem para eliminar al alumno del registro. */
    public interface OnLongClickAlumno {
        void onEliminar(AlumnoAsistente alumno);
    }

    private final List<AlumnoAsistente> alumnos = new ArrayList<>();
    private final Set<String> idsNuevos = new HashSet<>();
    private final Set<String> idsAnteriores = new HashSet<>();
    private OnLongClickAlumno listener;

    public void setOnLongClickAlumno(OnLongClickAlumno listener) {
        this.listener = listener;
    }

    public void actualizarLista(List<AlumnoAsistente> lista) {
        Set<String> idsActuales = new HashSet<>();
        for (AlumnoAsistente alumno : lista) {
            idsActuales.add(alumno.getId());
            if (!idsAnteriores.contains(alumno.getId())) {
                idsNuevos.add(alumno.getId());
            }
        }
        idsAnteriores.clear();
        idsAnteriores.addAll(idsActuales);
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
        holder.tvHora.setText(SesionRepository.formatearHoraConFecha(alumno.getHoraRegistro()));

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onEliminar(alumnos.get(pos));
            }
            return true;
        });

        // Animación de entrada para alumnos recién registrados.
        if (idsNuevos.remove(alumno.getId())) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(80f);
            holder.itemView.animate().alpha(1f).translationY(0f).setDuration(350).start();
        } else {
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
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