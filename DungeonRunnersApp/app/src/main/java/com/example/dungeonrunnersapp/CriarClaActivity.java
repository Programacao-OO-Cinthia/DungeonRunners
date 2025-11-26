package com.example.dungeonrunnersapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import Type.Player;
import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class CriarClaActivity extends AppCompatActivity {

    private EditText editNomeCla, editDescricaoCla;
    private Button btnCriarCla;
    private Player playerAtual;
    private SupabaseClient supabaseClient;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_cla);

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        editNomeCla = findViewById(R.id.editNomeCla);
        editDescricaoCla = findViewById(R.id.editDescricaoCla);
        btnCriarCla = findViewById(R.id.btnCriarCla);

        supabaseClient = new SupabaseClient();
        prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
        carregarPlayer();

        btnVoltar.setOnClickListener(v -> finish());

        btnCriarCla.setOnClickListener(v -> criarCla());
    }

    private void carregarPlayer() {
        String id = prefs.getString("userId", "");
        String nickname = prefs.getString("nickname", "Runner");
        int nivel = prefs.getInt("nivel", 1);
        int idCla = prefs.getInt("idCla", 0);
        double kmTotal = prefs.getFloat("kmPercorridos", 0.0f);
        int fitCoins = prefs.getInt("fitcoins", 100);
        int xp = prefs.getInt("xp", 0);

        playerAtual = new Player(id, nickname, nivel, idCla, kmTotal, fitCoins, xp);
    }

    private void criarCla() {
        String nome = editNomeCla.getText().toString().trim();
        String descricao = editDescricaoCla.getText().toString().trim();

        if (nome.isEmpty()) {
            Toast.makeText(this, "Digite um nome para o clã", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playerAtual.temGuilda()) {
            Toast.makeText(this, "Você já está em um clã", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCriarCla.setEnabled(false);
        btnCriarCla.setText("CRIANDO...");

        try {
            JSONObject claData = new JSONObject();
            claData.put("nome", nome);
            claData.put("descricao", descricao.isEmpty() ? "Clã criado por " + playerAtual.getNickname() : descricao);
            claData.put("pontuacao", 0);
            claData.put("membros_count", 1);

            supabaseClient.insert("cla", claData, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CriarClaActivity.this, "Erro de conexão", Toast.LENGTH_SHORT).show();
                        btnCriarCla.setEnabled(true);
                        btnCriarCla.setText("CRIAR CLÃ");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Busca o ID do clã criado agora e associa o jogador
                        associarJogadorAoCla(nome);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(CriarClaActivity.this, "Erro ao criar clã", Toast.LENGTH_SHORT).show();
                            btnCriarCla.setEnabled(true);
                            btnCriarCla.setText("CRIAR CLÃ");
                        });
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Erro interno", Toast.LENGTH_SHORT).show();
            btnCriarCla.setEnabled(true);
            btnCriarCla.setText("CRIAR CLÃ");
        }
    }

    private void associarJogadorAoCla(String nomeCla) {
        // Busca o clã pelo nome para pegar o ID
        String query = "cla?select=id&nome=eq." + nomeCla;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(CriarClaActivity.this, "Erro ao associar clã", Toast.LENGTH_SHORT).show();
                    btnCriarCla.setEnabled(true);
                    btnCriarCla.setText("CRIAR CLÃ");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        org.json.JSONArray array = new org.json.JSONArray(responseBody);

                        if (array.length() > 0) {
                            org.json.JSONObject claData = array.getJSONObject(0);
                            int claId = claData.getInt("id");

                            // Atualiza o perfil do jogador com o cla_id
                            atualizarPerfilJogador(claId, nomeCla);
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(CriarClaActivity.this, "Erro ao processar clã", Toast.LENGTH_SHORT).show();
                            btnCriarCla.setEnabled(true);
                            btnCriarCla.setText("CRIAR CLÃ");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(CriarClaActivity.this, "Erro ao buscar clã", Toast.LENGTH_SHORT).show();
                        btnCriarCla.setEnabled(true);
                        btnCriarCla.setText("CRIAR CLÃ");
                    });
                }
            }
        });
    }

    private void atualizarPerfilJogador(int claId, String nomeCla) {
        try {
            JSONObject perfilData = new JSONObject();
            perfilData.put("cla_id", claId);

            String userId = playerAtual.getId();
            supabaseClient.update("perfis", "id=eq." + userId, perfilData, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CriarClaActivity.this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show();
                        btnCriarCla.setEnabled(true);
                        btnCriarCla.setText("CRIAR CLÃ");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // Atualiza SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("idCla", claId);
                            editor.putString("guildaNome", nomeCla);
                            editor.apply();

                            Toast.makeText(CriarClaActivity.this, "Clã criado com sucesso!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(CriarClaActivity.this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show();
                            btnCriarCla.setEnabled(true);
                            btnCriarCla.setText("CRIAR CLÃ");
                        }
                    });
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(CriarClaActivity.this, "Erro interno", Toast.LENGTH_SHORT).show();
                btnCriarCla.setEnabled(true);
                btnCriarCla.setText("CRIAR CLÃ");
            });
        }
    }
}
