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

public class GuildaAdapter extends RecyclerView.Adapter<GuildaAdapter.GuildaViewHolder> {

    private List<Cla> guildas;
    private OnGuildaClickListener listener;

    public interface OnGuildaClickListener {
        void onEntrarGuildaClick(Cla guilda);
    }

    public GuildaAdapter(List<Cla> guildas, OnGuildaClickListener listener) {
        this.guildas = guildas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GuildaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guilda, parent, false);
        return new GuildaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuildaViewHolder holder, int position) {
        Cla guilda = guildas.get(position);
        holder.bind(guilda, listener);
    }

    @Override
    public int getItemCount() {
        return guildas.size();
    }

    public void setGuildas(List<Cla> guildas) {
        this.guildas = guildas;
        notifyDataSetChanged();
    }

    static class GuildaViewHolder extends RecyclerView.ViewHolder {
        private TextView txtNomeGuilda;
        private TextView txtDescricao;
        private TextView txtInfo;
        private Button btnEntrar;

        public GuildaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNomeGuilda = itemView.findViewById(R.id.txtNomeGuildaItem);
            txtDescricao = itemView.findViewById(R.id.txtDescricaoGuilda);
            txtInfo = itemView.findViewById(R.id.txtInfoGuilda);
            btnEntrar = itemView.findViewById(R.id.btnEntrarGuilda);
        }

        public void bind(Cla guilda, OnGuildaClickListener listener) {
            txtNomeGuilda.setText(guilda.getNome());
            txtDescricao.setText(guilda.getDescricao());

            // CORREÇÃO: Mostra pontuação em vez de km
            txtInfo.setText(String.format(" %d membros | 🏆 %d pontos",
                    guilda.getMembrosCount(), guilda.getPontuacao()));

            btnEntrar.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEntrarGuildaClick(guilda);
                }
            });
        }
    }
}