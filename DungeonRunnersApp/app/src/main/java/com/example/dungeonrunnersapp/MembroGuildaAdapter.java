package com.example.dungeonrunnersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import Type.MembroGuilda;
import java.util.List;

public class MembroGuildaAdapter extends RecyclerView.Adapter<MembroGuildaAdapter.MembroViewHolder> {

    private List<MembroGuilda> membros;

    public MembroGuildaAdapter(List<MembroGuilda> membros) {
        this.membros = membros;
    }

    @NonNull
    @Override
    public MembroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_membro_guilda, parent, false);
        return new MembroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MembroViewHolder holder, int position) {
        MembroGuilda membro = membros.get(position);
        holder.bind(membro);
    }

    @Override
    public int getItemCount() {
        return membros.size();
    }

    public void setMembros(List<MembroGuilda> membros) {
        this.membros = membros;
        notifyDataSetChanged();
    }

    static class MembroViewHolder extends RecyclerView.ViewHolder {
        private TextView txtNomeMembro;
        private TextView txtNivelMembro;
        private TextView txtKmMembro;
        private TextView txtCargoMembro;

        public MembroViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNomeMembro = itemView.findViewById(R.id.txtNomeMembro);
            txtNivelMembro = itemView.findViewById(R.id.txtNivelMembro);
            txtKmMembro = itemView.findViewById(R.id.txtKmMembro);
            txtCargoMembro = itemView.findViewById(R.id.txtCargoMembro);
        }

        public void bind(MembroGuilda membro) {
            txtNomeMembro.setText(membro.getNickname());
            txtNivelMembro.setText("Nível " + membro.getNivel());
            txtKmMembro.setText(String.format("%.2f km", membro.getKmTotal()));
            txtCargoMembro.setText(membro.getCargo());

            // Destacar o cargo de líder
            if ("Líder".equals(membro.getCargo())) {
                txtCargoMembro.setTextColor(itemView.getContext().getColor(R.color.gold));
            } else {
                txtCargoMembro.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }
        }
    }
}