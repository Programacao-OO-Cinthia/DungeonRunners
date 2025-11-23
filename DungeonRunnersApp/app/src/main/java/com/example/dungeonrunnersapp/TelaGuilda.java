package com.example.dungeonrunnersapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class TelaGuilda extends AppCompatActivity implements GuildaAdapter.OnClaClickListener {

    private static final String TAG = "TelaGuilda";
    private static final int REQUEST_CRIAR_CLA = 1;

    private GuildaAdapter claAdapter;
    private List<Cla> clasDisponiveis = new ArrayList<>();

    // Views
    private TextView txtNomeCla, txtKmTotalCla, txtInfoCla, txtMensagemErro;
    private RecyclerView recyclerMembros, recyclerClas;
    private Button btnCriarCla, btnSairCla;
    private View cardHeaderCla, cardMembros, cardClasDisponiveis;

    // Adapters
    private MembroGuildaAdapter membroAdapter;

    // Data
    private List<MembroGuilda> membrosCla = new ArrayList<>();
    private Player playerAtual;
    private Cla claAtual;

    // Supabase
    private SupabaseClient supabaseClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_guilda);

        inicializarViews();
        inicializarDados();
        carregarPlayerDoBanco();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sempre recarrega do banco quando a tela volta ao foco
        carregarPlayerDoBanco();
    }

    private void inicializarViews() {
        // Header do Clã
        cardHeaderCla = findViewById(R.id.cardHeaderGuilda);
        txtNomeCla = findViewById(R.id.txtNomeGuilda);
        txtKmTotalCla = findViewById(R.id.txtKmTotalGuilda);
        txtInfoCla = findViewById(R.id.txtInfoGuilda);

        // Lista de Membros
        cardMembros = findViewById(R.id.cardMembros);
        recyclerMembros = findViewById(R.id.recyclerMembros);

        // Clãs Disponíveis
        cardClasDisponiveis = findViewById(R.id.cardGuildasDisponiveis);
        btnCriarCla = findViewById(R.id.btnCriarGuilda);
        btnSairCla = findViewById(R.id.btnSairCla);
        recyclerClas = findViewById(R.id.recyclerGuildas);

        // Mensagem de Erro
        txtMensagemErro = findViewById(R.id.txtMensagemErro);

        // Configurar RecyclerView de Membros
        recyclerMembros.setLayoutManager(new LinearLayoutManager(this));
        membroAdapter = new MembroGuildaAdapter(membrosCla);
        recyclerMembros.setAdapter(membroAdapter);

        // Configurar RecyclerView de Clãs
        recyclerClas.setLayoutManager(new LinearLayoutManager(this));
        claAdapter = new GuildaAdapter(clasDisponiveis, this);
        recyclerClas.setAdapter(claAdapter);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        btnCriarCla.setOnClickListener(v -> {
            Intent intent = new Intent(TelaGuilda.this, CriarClaActivity.class);
            startActivityForResult(intent, REQUEST_CRIAR_CLA);
        });

        btnSairCla.setOnClickListener(v -> sairDoCla());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CRIAR_CLA && resultCode == RESULT_OK) {
            Toast.makeText(this, "Clã criado com sucesso!", Toast.LENGTH_SHORT).show();
            carregarPlayerDoBanco();
        }
    }

    private void inicializarDados() {
        supabaseClient = new SupabaseClient();
        prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
    }

    /**
     * Carrega o player diretamente do banco de dados Supabase
     * Isso garante que sempre tenhamos os dados mais atualizados
     */
    private void carregarPlayerDoBanco() {
        String userId = prefs.getString("userId", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "Erro: usuário não identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String query = "perfis?select=*&id=eq." + userId;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro ao carregar player: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(TelaGuilda.this, "Erro ao carregar dados do jogador", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Resposta player: " + responseBody);
                        JSONArray array = new JSONArray(responseBody);

                        if (array.length() > 0) {
                            JSONObject perfilData = array.getJSONObject(0);

                            String id = perfilData.getString("id");
                            String nickname = perfilData.optString("nickname", "Runner");
                            int nivel = perfilData.optInt("nivel", 1);
                            int claId = perfilData.optInt("cla_id", 0);
                            double kmTotal = perfilData.optDouble("kmTotal", 0.0);
                            int fitCoins = perfilData.optInt("fitcoins", 100);
                            int xp = perfilData.optInt("xp", 0);

                            // Atualiza SharedPreferences com os dados do banco
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("userId", id);
                            editor.putString("nickname", nickname);
                            editor.putInt("nivel", nivel);
                            editor.putInt("idCla", claId);
                            editor.putFloat("kmPercorridos", (float) kmTotal);
                            editor.putInt("fitcoins", fitCoins);
                            editor.putInt("xp", xp);
                            editor.apply();

                            playerAtual = new Player(id, nickname, nivel, claId, kmTotal, fitCoins, xp);

                            Log.d(TAG, "Player carregado do banco - ID: " + id + ", Clã ID: " + claId);

                            runOnUiThread(() -> {
                                verificarClaPlayer();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(TelaGuilda.this, "Perfil não encontrado", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar player: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(TelaGuilda.this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Resposta não sucedida: " + response.code());
                }
            }
        });
    }

    private void verificarClaPlayer() {
        if (playerAtual != null && playerAtual.temGuilda()) {
            Log.d(TAG, "Player tem clã: " + playerAtual.getGuildaId());
            carregarClaAtual();
        } else {
            Log.d(TAG, "Player não tem clã");
            mostrarListaClas();
            carregarClasDisponiveis();
        }
    }

    private void carregarClaAtual() {
        int claId = playerAtual.getGuildaId();
        String query = "cla?select=*&id=eq." + claId;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro ao carregar clã: " + e.getMessage());
                runOnUiThread(() -> {
                    mostrarErro("Erro ao carregar informações do clã");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Resposta clã: " + responseBody);
                        JSONArray array = new JSONArray(responseBody);

                        if (array.length() > 0) {
                            JSONObject claData = array.getJSONObject(0);

                            int id = claData.getInt("id");
                            String nome = claData.getString("nome");
                            String descricao = claData.optString("descricao", "");
                            int pontuacao = claData.optInt("pontuacao", 0);
                            int membrosCount = claData.optInt("membros_count", 0);

                            claAtual = new Cla(id, nome, descricao, membrosCount, pontuacao, "");

                            // Atualiza o nome do clã no SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("guildaNome", nome);
                            editor.apply();

                            runOnUiThread(() -> {
                                carregarMembrosCla();
                            });
                        } else {
                            Log.w(TAG, "Clã não encontrado no banco");
                            runOnUiThread(() -> {
                                // Se o clã não existe mais, limpa a referência
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putInt("idCla", 0);
                                editor.remove("guildaNome");
                                editor.apply();

                                mostrarListaClas();
                                carregarClasDisponiveis();
                                Toast.makeText(TelaGuilda.this, "Clã não encontrado", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar clã: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void carregarMembrosCla() {
        int claId = playerAtual.getGuildaId();
        String query = "perfis?select=id,nickname,nivel,kmTotal,xp&cla_id=eq." + claId + "&order=kmTotal.desc";

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro ao carregar membros: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Resposta membros: " + responseBody);
                        JSONArray array = new JSONArray(responseBody);

                        membrosCla.clear();
                        double kmTotalCla = 0.0;

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject membroData = array.getJSONObject(i);

                            String id = membroData.getString("id");
                            String nickname = membroData.getString("nickname");
                            int nivel = membroData.getInt("nivel");
                            double kmTotal = membroData.getDouble("kmTotal");
                            int xp = membroData.getInt("xp");

                            // Soma os kms de todos os membros
                            kmTotalCla += kmTotal;

                            // O primeiro membro (maior km) é o líder
                            boolean isLider = i == 0;
                            String cargo = isLider ? "Líder" : "Membro";

                            MembroGuilda membro = new MembroGuilda(id, nickname, nivel, claId, kmTotal, xp, cargo);
                            membrosCla.add(membro);
                        }

                        final double kmFinal = kmTotalCla;

                        runOnUiThread(() -> {
                            membroAdapter.setMembros(membrosCla);
                            mostrarClaAtual(kmFinal);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar membros: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void mostrarClaAtual(double kmTotal) {
        // Esconde a lista de clãs disponíveis
        cardClasDisponiveis.setVisibility(View.GONE);
        btnCriarCla.setVisibility(View.GONE);
        txtMensagemErro.setVisibility(View.GONE);

        // Mostra header, lista de membros e botão sair
        cardHeaderCla.setVisibility(View.VISIBLE);
        cardMembros.setVisibility(View.VISIBLE);
        btnSairCla.setVisibility(View.VISIBLE);

        // Atualiza informações do clã
        if (claAtual != null) {
            txtNomeCla.setText(claAtual.getNome());
            txtKmTotalCla.setText(String.format("%.2f km", kmTotal));
            txtInfoCla.setText(String.format("Membros: %d", membrosCla.size()));
        }
    }

    private void sairDoCla() {
        if (playerAtual == null || !playerAtual.temGuilda()) {
            Toast.makeText(this, "Você não está em um clã", Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirmação antes de sair
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sair do Clã")
                .setMessage("Tem certeza que deseja sair do clã " + claAtual.getNome() + "?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    executarSaidaDoCla();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarSaidaDoCla() {
        try {
            JSONObject perfilData = new JSONObject();
            perfilData.put("cla_id", JSONObject.NULL);

            String userId = playerAtual.getId();
            supabaseClient.update("perfis", "id=eq." + userId, perfilData, new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    Log.e(TAG, "Erro ao sair do clã: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(TelaGuilda.this, "Erro ao sair do clã", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // Limpa SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("idCla", 0);
                            editor.remove("guildaNome");
                            editor.apply();

                            Toast.makeText(TelaGuilda.this, "Você saiu do clã", Toast.LENGTH_SHORT).show();

                            // Recarrega os dados
                            carregarPlayerDoBanco();
                        } else {
                            Toast.makeText(TelaGuilda.this, "Erro ao sair do clã", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar JSON: " + e.getMessage());
            Toast.makeText(this, "Erro interno", Toast.LENGTH_SHORT).show();
        }
    }

    private void carregarClasDisponiveis() {
        String query = "cla?select=*&order=pontuacao.desc";

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Erro ao carregar clãs: " + e.getMessage());
                runOnUiThread(() -> {
                    mostrarErro("Erro ao carregar clãs disponíveis");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Resposta clãs: " + responseBody);
                        JSONArray array = new JSONArray(responseBody);

                        clasDisponiveis.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject claData = array.getJSONObject(i);

                            int id = claData.getInt("id");
                            String nome = claData.getString("nome");
                            String descricao = claData.optString("descricao", "Sem descrição");
                            int pontuacao = claData.optInt("pontuacao", 0);
                            int membrosCount = claData.optInt("membros_count", 0);

                            Cla cla = new Cla(id, nome, descricao, membrosCount, pontuacao, "");
                            clasDisponiveis.add(cla);
                        }

                        runOnUiThread(() -> {
                            claAdapter.setGuildas(clasDisponiveis);

                            TextView txtSemClas = findViewById(R.id.txtSemGuildas);
                            if (clasDisponiveis.isEmpty()) {
                                txtSemClas.setVisibility(View.VISIBLE);
                                txtSemClas.setText("Nenhum clã disponível");
                            } else {
                                txtSemClas.setVisibility(View.GONE);
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar clãs: " + e.getMessage());
                        runOnUiThread(() -> mostrarErro("Erro ao processar clãs"));
                    }
                } else {
                    Log.e(TAG, "Resposta não sucedida: " + response.code());
                    runOnUiThread(() -> mostrarErro("Erro ao buscar clãs"));
                }
            }
        });
    }

    @Override
    public void onEntrarClaClick(Cla cla) {
        entrarNoCla(cla);
    }

    private void entrarNoCla(Cla cla) {
        if (playerAtual == null) {
            Toast.makeText(this, "Erro: jogador não carregado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playerAtual.temGuilda()) {
            Toast.makeText(this, "Você já está em um clã", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject perfilData = new JSONObject();
            perfilData.put("cla_id", cla.getId());

            String userId = playerAtual.getId();
            supabaseClient.update("perfis", "id=eq." + userId, perfilData, new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    Log.e(TAG, "Erro ao entrar no clã: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(TelaGuilda.this, "Erro ao entrar no clã", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(TelaGuilda.this, "Entrou no clã " + cla.getNome(), Toast.LENGTH_SHORT).show();

                            // Recarrega os dados do banco para garantir sincronização
                            carregarPlayerDoBanco();
                        } else {
                            Toast.makeText(TelaGuilda.this, "Erro ao entrar no clã", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erro ao criar JSON: " + e.getMessage());
            Toast.makeText(this, "Erro interno", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarListaClas() {
        // Esconde informações do clã
        cardHeaderCla.setVisibility(View.GONE);
        cardMembros.setVisibility(View.GONE);
        txtMensagemErro.setVisibility(View.GONE);
        btnSairCla.setVisibility(View.GONE);

        // Mostra lista de clãs disponíveis
        cardClasDisponiveis.setVisibility(View.VISIBLE);
        btnCriarCla.setVisibility(View.VISIBLE);
    }

    private void mostrarErro(String mensagem) {
        txtMensagemErro.setText(mensagem);
        txtMensagemErro.setVisibility(View.VISIBLE);
    }
}