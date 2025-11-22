package com.example.dungeonrunnersapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import Type.Cla;
import Type.MembroGuilda;
import Type.Player;
import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TelaGuilda extends AppCompatActivity implements GuildaAdapter.OnGuildaClickListener {

    private static final String TAG = "TelaGuilda";
    private static final int GUILDAS_POR_PAGINA = 5;

    // Views
    private ImageButton btnVoltar;
    private CardView cardHeaderGuilda, cardMembros, cardGuildasDisponiveis;
    private RecyclerView recyclerMembros, recyclerGuildas;
    private TextView txtNomeGuilda, txtKmTotalGuilda, txtInfoGuilda, txtPaginaAtual, txtMensagemErro;
    private Button btnPaginaAnterior, btnProximaPagina, btnCriarGuilda;
    private ProgressBar progressBar;

    // Adapters
    private GuildaAdapter guildaAdapter;
    private MembroGuildaAdapter membroAdapter;

    // Data
    private List<Cla> guildasDisponiveis = new ArrayList<>();
    private List<MembroGuilda> membrosGuilda = new ArrayList<>();
    private Player playerAtual;
    private Cla guildaAtual;

    // Paginação
    private int paginaAtual = 1;
    private int totalGuildas = 0;

    // Supabase
    private SupabaseClient supabaseClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_guilda);

        Log.d(TAG, "=== TELA GUILDA INICIADA ===");

        inicializarViews();
        inicializarDados();
        carregarPlayer();
        verificarGuildaPlayer();
    }

    private void inicializarViews() {
        btnVoltar = findViewById(R.id.btnVoltar);
        cardHeaderGuilda = findViewById(R.id.cardHeaderGuilda);
        cardMembros = findViewById(R.id.cardMembros);
        cardGuildasDisponiveis = findViewById(R.id.cardGuildasDisponiveis);

        recyclerMembros = findViewById(R.id.recyclerMembros);
        recyclerGuildas = findViewById(R.id.recyclerGuildas);

        txtNomeGuilda = findViewById(R.id.txtNomeGuilda);
        txtKmTotalGuilda = findViewById(R.id.txtKmTotalGuilda);
        txtInfoGuilda = findViewById(R.id.txtInfoGuilda);
        txtPaginaAtual = findViewById(R.id.txtPaginaAtual);
        txtMensagemErro = findViewById(R.id.txtMensagemErro);

        btnPaginaAnterior = findViewById(R.id.btnPaginaAnterior);
        btnProximaPagina = findViewById(R.id.btnProximaPagina);
        btnCriarGuilda = findViewById(R.id.btnCriarGuilda);

        progressBar = findViewById(R.id.progressBar);

        // Configurar RecyclerViews
        recyclerGuildas.setLayoutManager(new LinearLayoutManager(this));
        recyclerMembros.setLayoutManager(new LinearLayoutManager(this));

        guildaAdapter = new GuildaAdapter(guildasDisponiveis, this);
        recyclerGuildas.setAdapter(guildaAdapter);

        membroAdapter = new MembroGuildaAdapter(membrosGuilda);
        recyclerMembros.setAdapter(membroAdapter);

        // Listeners
        btnVoltar.setOnClickListener(v -> {
            finish(); // Fecha a tela atual e volta para a anterior
        });

        btnPaginaAnterior.setOnClickListener(v -> mudarPagina(paginaAtual - 1));
        btnProximaPagina.setOnClickListener(v -> mudarPagina(paginaAtual + 1));

        btnCriarGuilda.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidade de criar guilda em desenvolvimento", Toast.LENGTH_SHORT).show();
        });
    }

    private void inicializarDados() {
        supabaseClient = new SupabaseClient();
        prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
    }

    private void carregarPlayer() {
        SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);

        String id = prefs.getString("userId", "");
        String nickname = prefs.getString("nickname", "Runner");
        int nivel = prefs.getInt("nivel", 1);
        int idCla = prefs.getInt("idCla", 0); // JÁ EXISTE - é int
        double kmTotal = prefs.getFloat("kmPercorridos", 0.0f);
        int fitCoins = prefs.getInt("fitcoins", 100);
        int xp = prefs.getInt("xp", 0);
        String guildaNome = prefs.getString("guildaNome", "");

        // CORREÇÃO: Usa o idCla que já existe
        playerAtual = new Player(id, nickname, nivel, idCla, kmTotal, fitCoins, xp, guildaNome);

        Log.d(TAG, "Player carregado - ID Clã: " + playerAtual.getGuildaId() + " - Tem clã: " + playerAtual.temGuilda());
    }

    private void verificarGuildaPlayer() {
        mostrarLoading(true);

        if (playerAtual.temGuilda()) {
            Log.d(TAG, "Player tem guilda, carregando informações: " + playerAtual.getGuildaNome());
            carregarGuildaAtual();
        } else {
            Log.d(TAG, "Player não tem guilda, carregando lista de guildas");
            carregarGuildasDisponiveis();
        }
    }

    private void carregarGuildaAtual() {
        int claId = playerAtual.getGuildaId();

        // CORREÇÃO: Query mais simples
        String query = "cla?id=eq." + claId;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro: " + e.getMessage());
                runOnUiThread(() -> mostrarLoading(false));
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                runOnUiThread(() -> mostrarLoading(false));

                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray array = new JSONArray(responseBody);

                        if (array.length() > 0) {
                            JSONObject claData = array.getJSONObject(0);

                            int id = claData.getInt("id");
                            String nome = claData.getString("nome");
                            String descricao = claData.optString("descricao", "");
                            int pontuacao = claData.optInt("pontuacao", 0);
                            int membrosCount = claData.optInt("membros_count", 0);

                            guildaAtual = new Cla(id, nome, descricao, membrosCount, pontuacao, "");

                            runOnUiThread(() -> {
                                mostrarGuildaAtual();
                                carregarMembrosGuilda();
                            });
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Erro: " + e.getMessage());
                    }
                }
            }
        });
    }

    private String buscarNomeLider(String liderId) {
        // Em uma implementação real, você buscaria o nome do líder na tabela de perfis
        // Por enquanto, retornamos um placeholder
        return "Líder";
    }

    private void carregarMembrosGuilda() {
        int claId = playerAtual.getGuildaId();

        // CORREÇÃO: Query mais simples
        String query = "perfis?cla_id=eq." + claId;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray array = new JSONArray(responseBody);

                        membrosGuilda.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject membroData = array.getJSONObject(i);

                            String id = membroData.getString("id");
                            String nickname = membroData.optString("nickname", "Jogador");
                            int nivel = membroData.optInt("nivel", 1);
                            double kmTotal = membroData.optDouble("kmTotal", 0);
                            int xp = membroData.optInt("xp", 0);

                            MembroGuilda membro = new MembroGuilda(id, nickname, nivel, 0 ,kmTotal, xp, "Membro");
                            membrosGuilda.add(membro);
                        }

                        runOnUiThread(() -> {
                            membroAdapter.setMembros(membrosGuilda);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Erro: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void carregarGuildasDisponiveis() {
        // CORREÇÃO: Query mais simples, apenas campos básicos
        String query = "cla?select=*";

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro de conexão: " + e.getMessage());
                runOnUiThread(() -> {
                    mostrarLoading(false);
                    mostrarErro("Erro de conexão");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                runOnUiThread(() -> mostrarLoading(false));

                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Resposta: " + responseBody);

                        JSONArray array = new JSONArray(responseBody);

                        guildasDisponiveis.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject claData = array.getJSONObject(i);

                            // CORREÇÃO: Campos básicos que existem na tabela
                            int id = claData.getInt("id");
                            String nome = claData.getString("nome");
                            String descricao = claData.optString("descricao", "Sem descrição");
                            int pontuacao = claData.optInt("pontuacao", 0);
                            int membrosCount = claData.optInt("membros_count", 0);

                            Cla cla = new Cla(id, nome, descricao, membrosCount, pontuacao, "");
                            guildasDisponiveis.add(cla);
                        }

                        runOnUiThread(() -> {
                            guildaAdapter.setGuildas(guildasDisponiveis);
                            atualizarControlesPagina();
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar: " + e.getMessage());
                        runOnUiThread(() -> mostrarErro("Erro ao carregar dados"));
                    }
                } else {
                    Log.e(TAG, "Erro HTTP: " + response.code());
                    runOnUiThread(() -> mostrarErro("Erro " + response.code()));
                }
            }
        });
    }

    private void mostrarGuildaAtual() {
        cardHeaderGuilda.setVisibility(View.VISIBLE);
        cardMembros.setVisibility(View.VISIBLE);
        cardGuildasDisponiveis.setVisibility(View.GONE);
        btnCriarGuilda.setVisibility(View.GONE);

        txtNomeGuilda.setText(guildaAtual.getNome());
        txtKmTotalGuilda.setText(String.format("%.2f km", guildaAtual.getKmTotal()));
        txtInfoGuilda.setText(String.format("Membros: %d | Criada em: %s",
                guildaAtual.getMembrosCount(), guildaAtual.getDataCriacao()));
    }

    private void mostrarListaGuildas() {
        cardHeaderGuilda.setVisibility(View.GONE);
        cardMembros.setVisibility(View.GONE);
        cardGuildasDisponiveis.setVisibility(View.VISIBLE);
        btnCriarGuilda.setVisibility(View.VISIBLE);
    }

    private void mudarPagina(int novaPagina) {
        if (novaPagina < 1) return;

        paginaAtual = novaPagina;
        mostrarLoading(true);
        carregarGuildasDisponiveis();
    }

    private void atualizarControlesPagina() {
        txtPaginaAtual.setText("Página " + paginaAtual);
        btnPaginaAnterior.setEnabled(paginaAtual > 1);
        // Note: Não temos o total de páginas, então sempre habilitamos o próximo
    }

    private void mostrarLoading(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        if (mostrar) {
            txtMensagemErro.setVisibility(View.GONE);
        }
    }

    private void mostrarErro(String mensagem) {
        txtMensagemErro.setText(mensagem);
        txtMensagemErro.setVisibility(View.VISIBLE);
    }

    // Implementação do listener para entrar na guilda
    @Override
    public void onEntrarGuildaClick(Cla guilda) {
        Toast.makeText(this, "Entrando na guilda: " + guilda.getNome(), Toast.LENGTH_SHORT).show();

        // Aqui você implementaria a lógica para entrar na guilda
        // Atualizando o perfil do jogador com o ID da guilda
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Tela guilda destruída");
    }
}