package com.example.dungeonrunnersapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import Type.RankingManager;
import Type.RankingPlayer;

public class TelaRanking extends AppCompatActivity {

    private static final String TAG = "TelaRanking";

    private RecyclerView recyclerViewRanking;
    private RankingAdapter rankingAdapter;
    private RankingManager rankingManager;
    private TextView txtPosicaoUsuario;
    private Button btnVoltar; // CORREÇÃO: Mudou de ImageButton para Button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_ranking);

        rankingManager = new RankingManager();
        inicializarComponentes();
        configurarListeners();
        carregarRanking();
    }

    private void inicializarComponentes() {
        recyclerViewRanking = findViewById(R.id.recyclerViewRanking);
        txtPosicaoUsuario = findViewById(R.id.txtPosicaoUsuario);
        btnVoltar = findViewById(R.id.btnVoltarRanking); // CORREÇÃO: Agora é Button

        // Configurar RecyclerView
        recyclerViewRanking.setLayoutManager(new LinearLayoutManager(this));
        rankingAdapter = new RankingAdapter();
        recyclerViewRanking.setAdapter(rankingAdapter);
    }

    private void configurarListeners() {
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void carregarRanking() {
        // Carregar ranking geral
        rankingManager.carregarRankingGeral(new RankingManager.RankingCallback() {
            @Override
            public void onRankingCarregado(List<RankingPlayer> ranking) {
                runOnUiThread(() -> {
                    rankingAdapter.setRanking(ranking);
                    Log.d(TAG, "Ranking carregado: " + ranking.size() + " jogadores");
                });
            }

            @Override
            public void onRankingError(String erro) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erro ao carregar ranking: " + erro);
                    // Mostrar mensagem de erro para o usuário
                    txtPosicaoUsuario.setText("Erro ao carregar ranking");
                });
            }
        });

        // Buscar posição do usuário atual
        String userId = getSharedPreferences("DungeonRunners", MODE_PRIVATE)
                .getString("userId", "");

        if (!userId.isEmpty()) {
            rankingManager.buscarPosicaoJogador(userId, new RankingManager.PosicaoCallback() {
                @Override
                public void onPosicaoEncontrada(int posicao) {
                    runOnUiThread(() -> {
                        txtPosicaoUsuario.setText("Sua posição: #" + posicao);
                    });
                }

                @Override
                public void onPosicaoError(String erro) {
                    runOnUiThread(() -> {
                        txtPosicaoUsuario.setText("Posição não disponível");
                        Log.e(TAG, "Erro ao buscar posição: " + erro);
                    });
                }
            });
        } else {
            txtPosicaoUsuario.setText("Usuário não identificado");
        }
    }
}