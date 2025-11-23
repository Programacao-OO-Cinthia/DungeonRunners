package com.example.dungeonrunnersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import Type.Cla;
import java.util.List;

public class GuildaAdapter extends RecyclerView.Adapter<GuildaAdapter.ClaViewHolder> {

    private List<Cla> clas;
    private OnClaClickListener listener;

    public interface OnClaClickListener {
        void onEntrarClaClick(Cla cla);
    }

    public GuildaAdapter(List<Cla> clas, OnClaClickListener listener) {
        this.clas = clas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guilda, parent, false);
        return new ClaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClaViewHolder holder, int position) {
        Cla cla = clas.get(position);
        holder.bind(cla, listener);
    }

    @Override
    public int getItemCount() {
        return clas != null ? clas.size() : 0;
    }

    public void setGuildas(List<Cla> clas) {
        this.clas = clas;
        notifyDataSetChanged();
    }

    static class ClaViewHolder extends RecyclerView.ViewHolder {
        private TextView txtNomeCla;
        private TextView txtDescricao;
        private TextView txtInfo;
        private Button btnEntrar;

        public ClaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNomeCla = itemView.findViewById(R.id.txtNomeGuildaItem);
            txtDescricao = itemView.findViewById(R.id.txtDescricaoGuilda);
            txtInfo = itemView.findViewById(R.id.txtInfoGuilda);
            btnEntrar = itemView.findViewById(R.id.btnEntrarGuilda);
        }

        public void bind(Cla cla, OnClaClickListener listener) {
            txtNomeCla.setText(cla.getNome());
            txtDescricao.setText(cla.getDescricao());
            txtInfo.setText(String.format("👥 %d membros | 🏆 %d pontos",
                    cla.getMembrosCount(), cla.getPontuacao()));

            btnEntrar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEntrarClaClick(cla);
                }
            });
        }
    }
}