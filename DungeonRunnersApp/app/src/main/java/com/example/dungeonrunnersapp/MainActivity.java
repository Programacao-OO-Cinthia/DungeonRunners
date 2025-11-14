package com.example.dungeonrunnersapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnEntrarLogin;
    private Button btnCadastrarLogin;
    private EditText edtUsuario;
    private EditText edtSenha;

    private SupabaseClient supabaseClient;

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

        // Inicializar cliente Supabase
        supabaseClient = new SupabaseClient();

        // Inicializar componentes
        inicializarComponentes();

        // Configurar listeners
        configurarListeners();
    }

    private void inicializarComponentes() {
        btnEntrarLogin = findViewById(R.id.btnEntrarLogin);
        btnCadastrarLogin = findViewById(R.id.btnCadastrarLogin);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtSenha = findViewById(R.id.edtSenha);
    }

    private void configurarListeners() {
        btnEntrarLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarLogin();
            }
        });

        btnCadastrarLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mudarTelaCadastro();
            }
        });
    }

    private void mudarTelaCadastro() {
        Intent intent = new Intent(MainActivity.this, TelaCadastro.class);
        startActivity(intent);
    }

    private void realizarLogin() {
        String usuario = edtUsuario.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        // Validação básica
        if (usuario.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Desabilitar botão durante o login
        btnEntrarLogin.setEnabled(false);

        // Buscar usuário no banco
        buscarUsuarioNoBanco(usuario, senha);
    }

    private void buscarUsuarioNoBanco(String usuario, String senha) {
        // Busca por nickname ou nome
        String query = "perfis?or=(nickname.eq." + usuario + ",nome.eq." + usuario + ")";

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnEntrarLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "Erro de conexão: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    btnEntrarLogin.setEnabled(true);

                    try {
                        JSONArray usuarios = new JSONArray(responseBody);

                        if (usuarios.length() == 0) {
                            Toast.makeText(MainActivity.this,
                                    "Usuário não encontrado",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject usuarioEncontrado = usuarios.getJSONObject(0);
                        String senhaArmazenada = usuarioEncontrado.getString("senha");

                        if (senha.equals(senhaArmazenada)) {
                            // Login bem-sucedido
                            salvarSessao(usuarioEncontrado);
                            Toast.makeText(MainActivity.this,
                                    "Login realizado com sucesso!",
                                    Toast.LENGTH_SHORT).show();

                            // Limpar campos
                            edtUsuario.setText("");
                            edtSenha.setText("");

                            // Navegar para próxima tela (crie uma TelaPrincipal)
                            // Intent intent = new Intent(MainActivity.this, TelaPrincipal.class);
                            // startActivity(intent);
                            // finish();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Senha incorreta",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao processar login: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void salvarSessao(JSONObject usuario) {
        try {
            SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("userId", usuario.getString("id"));
            editor.putString("nome", usuario.getString("nome"));
            editor.putString("nickname", usuario.getString("nickname"));
            editor.putInt("nivel", usuario.getInt("nivel"));
            editor.putInt("xp", usuario.getInt("xp"));
            editor.putInt("fitcoins", usuario.getInt("fitcoins"));
            editor.putBoolean("logado", true);

            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}