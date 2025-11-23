package com.example.dungeonrunnersapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import Type.Player;
import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button btnEntrarLogin; // botão de logar
    private Button btnCadastrarLogin; // botão que muda para a tela de cadastro
    private EditText edtUsuario; // campo de texto do nickname do usuário
    private EditText edtSenha; // campo de texto da senha do usuário

    private SupabaseClient supabaseClient; // Objeto de conexão com o supabase (banco de dados)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        supabaseClient = new SupabaseClient();
        inicializarComponentes(); // pega os objetos da interface gráfica
        configurarListeners(); // confiura as funções de cada botão
    }

    private void inicializarComponentes() {
        btnEntrarLogin = findViewById(R.id.btnEntrarLogin);
        btnCadastrarLogin = findViewById(R.id.btnCadastrarLogin);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtSenha = findViewById(R.id.edtSenha);
    }

    private void configurarListeners() {
        btnEntrarLogin.setOnClickListener(v -> realizarLogin()); // método de realizar o login
        btnCadastrarLogin.setOnClickListener(v -> mudarTelaCadastro()); // método de ir para a tela de cadastro
    }

    private void mudarTelaCadastro() {
        Intent intent = new Intent(MainActivity.this, TelaCadastro.class); // armazena a tela que deseja ir
        startActivity(intent); // vai para a tela de cadastro
    }

    private void realizarLogin() { // realiza o login dos usuários
        String usuario = edtUsuario.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        if (usuario.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEntrarLogin.setEnabled(false);
        buscarUsuarioNoBanco(usuario, senha);
    }

    private void buscarUsuarioNoBanco(String usuario, String senha) {
        // Buscar com select=* para pegar todos os campos, estava dando erro quando fazia busca sem todos os campos
        String query = "perfis?select=*&or=(nickname.eq." + usuario + ",nome.eq." + usuario + ")";

        Log.d(TAG, "Buscando usuário: " + usuario);

        supabaseClient.request("GET", query, null, new Callback() { // da get nas informações dos usuários
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnEntrarLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "Erro de conexão: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erro de conexão: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    btnEntrarLogin.setEnabled(true);

                    try {
                        JSONArray usuarios = new JSONArray(responseBody);

                        if (usuarios.length() == 0) { // valida a resposta, caso o nickname não seja encontrado
                            Toast.makeText(MainActivity.this,
                                    "Usuário não encontrado",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject usuarioJson = usuarios.getJSONObject(0);
                        String senhaArmazenada = usuarioJson.getString("senha");

                        if (senha.equals(senhaArmazenada)) { // valida o usuário encontrado
                            Player player = criarPlayerDoJSON(usuarioJson);
                            salvarSessao(player);

                            Toast.makeText(MainActivity.this,
                                    "Login realizado com sucesso!",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, TelaPrincipal.class);
                            startActivity(intent);
                            finish();
                        } else { // valida a resposta, caso o nickname esteja certo mas a senha não
                            Toast.makeText(MainActivity.this,
                                    "Senha incorreta",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) { // caso de erro
                        Toast.makeText(MainActivity.this,
                                "Erro ao processar login: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private Player criarPlayerDoJSON(JSONObject json) throws Exception {
        String id = json.getString("id");
        String nickname = json.getString("nickname");
        int nivel = json.getInt("nivel");
        int idCla = json.has("idcla") && !json.isNull("idcla") ? json.getInt("idcla") : 0;

        double kmTotal = 0.0;
        if (json.has("kmTotal") && !json.isNull("kmTotal")) {
            kmTotal = json.getDouble("kmTotal");
        }

        int fitCoins = json.getInt("fitcoins");
        int xp = json.getInt("xp");

        return new Player(id, nickname, nivel, idCla, kmTotal, fitCoins, xp);
    }

    private void salvarSessao(Player player) {
        try {
            SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("userId", player.getId());
            editor.putString("nickname", player.getNickname());
            editor.putInt("nivel", player.getNivel());
            editor.putInt("idCla", player.getIdCla());
            editor.putFloat("kmPercorridos", (float) player.getKmTotal());
            editor.putInt("fitcoins", player.getFitCoins());
            editor.putInt("xp", player.getXp());
            editor.putBoolean("logado", true);

            editor.apply();
        } catch (Exception e) {
        }
    }
}