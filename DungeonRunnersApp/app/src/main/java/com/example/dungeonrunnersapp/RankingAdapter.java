package com.example.dungeonrunnersapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Type.RankingPlayer;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private List<RankingPlayer> ranking = new ArrayList<>();

    public void setRanking(List<RankingPlayer> ranking) {
        this.ranking = ranking;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        RankingPlayer player = ranking.get(position);
        holder.bind(player);
    }

    @Override
    public int getItemCount() {
        return ranking.size();
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgMedalha;
        private TextView txtPosicao;
        private TextView txtNickname;
        private TextView txtNivel;
        private TextView txtKmTotal;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);

            // Buscar as views corretas
            imgMedalha = itemView.findViewById(R.id.imgMedalha);
            txtPosicao = itemView.findViewById(R.id.txtPosicao);
            txtNickname = itemView.findViewById(R.id.txtNicknameRanking);
            txtNivel = itemView.findViewById(R.id.txtNivelRanking);
            txtKmTotal = itemView.findViewById(R.id.txtKmRanking);

            // Log para debug
            if (imgMedalha == null) Log.e("RankingAdapter", "imgMedalha é nulo!");
            if (txtPosicao == null) Log.e("RankingAdapter", "txtPosicao é nulo!");
            if (txtNickname == null) Log.e("RankingAdapter", "txtNickname é nulo!");
            if (txtNivel == null) Log.e("RankingAdapter", "txtNivel é nulo!");
            if (txtKmTotal == null) Log.e("RankingAdapter", "txtKmTotal é nulo!");
        }

        public void bind(RankingPlayer player) {
            int posicao = player.getPosicao();

            // Configurar medalha ou posição numérica
            if (imgMedalha != null && txtPosicao != null) {
                switch (posicao) {
                    case 1:
                        configurarMedalha(R.drawable.medalha_ouro);
                        break;
                    case 2:
                        configurarMedalha(R.drawable.medalha_prata);
                        break;
                    case 3:
                        configurarMedalha(R.drawable.medalha_bronze);
                        break;
                    default:
                        configurarPosicaoNumerica(posicao);
                        break;
                }
            }

            // Configurar os outros dados
            if (txtNickname != null) {
                txtNickname.setText(player.getNickname());
            }

            if (txtNivel != null) {
                txtNivel.setText("Nv. " + player.getNivel());
            }

            if (txtKmTotal != null) {
                txtKmTotal.setText(String.format("%.2f km", player.getKmTotal()));
            }
        }

        private void configurarMedalha(int resourceId) {
            try {
                imgMedalha.setImageResource(resourceId);
                imgMedalha.setVisibility(View.VISIBLE);
                txtPosicao.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("RankingAdapter", "Erro ao carregar medalha: " + e.getMessage());
                // Fallback: mostrar número se a medalha não carregar
                configurarPosicaoNumerica(getAdapterPosition() + 1);
            }
        }

        private void configurarPosicaoNumerica(int posicao) {
            imgMedalha.setVisibility(View.GONE);
            txtPosicao.setVisibility(View.VISIBLE);
            txtPosicao.setText(String.valueOf(posicao));
        }
    }
}