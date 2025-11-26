package com.example.dungeonrunnersapp;

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
import java.util.UUID;

import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TelaCadastro extends AppCompatActivity {

    private EditText edtNome;
    private EditText edtNickname;
    private EditText edtSenhaCadastro;
    private EditText edtConfirmarSenha;
    private Button btnCadastrar;
    private Button btnVoltar;

    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_cadastro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar cliente Supabase
        supabaseClient = new SupabaseClient();

        // Inicializar os componentes
        inicializarComponentes();

        // Configurar os listeners dos botões
        configurarListeners();
    }

    private void inicializarComponentes() {
        edtNome = findViewById(R.id.edtNome);
        edtNickname = findViewById(R.id.edtNickname);
        edtSenhaCadastro = findViewById(R.id.edtSenhaCadastro);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltar = findViewById(R.id.btnVoltar);
    }

    private void configurarListeners() {
        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarCadastro();
            }
        });

        btnVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void realizarCadastro() {
        // Obter os valores dos campos
        String nome = edtNome.getText().toString().trim();
        String nickname = edtNickname.getText().toString().trim();
        String senha = edtSenhaCadastro.getText().toString();
        String confirmarSenha = edtConfirmarSenha.getText().toString();

        // Validações
        if (nome.isEmpty()) {
            Toast.makeText(this, "Preencha o nome completo", Toast.LENGTH_SHORT).show();
            edtNome.requestFocus();
            return;
        }

        if (nickname.isEmpty()) {
            Toast.makeText(this, "Preencha o nickname", Toast.LENGTH_SHORT).show();
            edtNickname.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            Toast.makeText(this, "Preencha a senha", Toast.LENGTH_SHORT).show();
            edtSenhaCadastro.requestFocus();
            return;
        }

        if (senha.length() < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show();
            edtSenhaCadastro.requestFocus();
            return;
        }

        if (confirmarSenha.isEmpty()) {
            Toast.makeText(this, "Confirme a senha", Toast.LENGTH_SHORT).show();
            edtConfirmarSenha.requestFocus();
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
            edtConfirmarSenha.requestFocus();
            return;
        }

        // Verificar se o nickname já existe antes de cadastrar
        verificarNicknameDisponivel(nome, nickname, senha);
    }

    // Verifica se o nickname já está em uso no banco de dados
    private void verificarNicknameDisponivel(String nome, String nickname, String senha) {
        btnCadastrar.setEnabled(false);
        btnCadastrar.setText("Verificando...");

        // Query para buscar usuários com o mesmo nickname
        String query = "perfis?select=id&nickname=eq." + nickname;

        supabaseClient.request("GET", query, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnCadastrar.setEnabled(true);
                    btnCadastrar.setText("Cadastrar");
                    Toast.makeText(TelaCadastro.this,
                            "Erro de conexão: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    btnCadastrar.setEnabled(true);
                    btnCadastrar.setText("Cadastrar");
                });

                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONArray usuarios = new JSONArray(responseBody);

                        if (usuarios.length() > 0) {
                            // Nickname já existe
                            runOnUiThread(() -> {
                                Toast.makeText(TelaCadastro.this,
                                        "❌ Este nickname já está em uso! Escolha outro.",
                                        Toast.LENGTH_LONG).show();
                                edtNickname.requestFocus();
                                edtNickname.selectAll();
                            });
                        } else {
                            // Nickname disponível
                            runOnUiThread(() -> {
                                cadastrarUsuarioNoBanco(nome, nickname, senha);
                            });
                        }
                    } else {
                        // Erro na consulta
                        runOnUiThread(() -> {
                            Toast.makeText(TelaCadastro.this,
                                    "Erro ao verificar nickname. Tente novamente.",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(TelaCadastro.this,
                                "Erro ao processar verificação: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void cadastrarUsuarioNoBanco(String nome, String nickname, String senha) {
        // Desabilitar o botão para evitar cliques múltiplos
        btnCadastrar.setEnabled(false);
        btnCadastrar.setText("Cadastrando...");

        try {
            String uuid = UUID.randomUUID().toString();

            JSONObject perfil = new JSONObject();
            perfil.put("id", uuid);
            perfil.put("nome", nome);
            perfil.put("nickname", nickname);
            perfil.put("senha", senha);
            perfil.put("nivel", 1);
            perfil.put("xp", 0);
            perfil.put("fitcoins", 100);
            perfil.put("kmTotal", 0.0);
            perfil.put("role", "usuario");

            supabaseClient.insert("perfis", perfil, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        btnCadastrar.setEnabled(true);
                        btnCadastrar.setText("Cadastrar");
                        Toast.makeText(TelaCadastro.this,
                                "Erro de conexão: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        btnCadastrar.setEnabled(true);
                        btnCadastrar.setText("Cadastrar");
                    });

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(TelaCadastro.this,
                                    "✅ Cadastro realizado com sucesso!",
                                    Toast.LENGTH_LONG).show();
                            limparCampos();

                            // Voltar para a tela de login após 1 segundo
                            new android.os.Handler().postDelayed(() -> finish(), 1000);
                        });
                    } else {
                        try {
                            String errorBody = response.body().string();
                            runOnUiThread(() -> {
                                // Verificar se é erro 
                                if (errorBody.contains("duplicate key") || errorBody.contains("unique")) {
                                    Toast.makeText(TelaCadastro.this,
                                            "❌ Este nickname já está em uso! Escolha outro.",
                                            Toast.LENGTH_LONG).show();
                                    edtNickname.requestFocus();
                                    edtNickname.selectAll();
                                } else {
                                    Toast.makeText(TelaCadastro.this,
                                            "Erro ao cadastrar: " + errorBody,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(TelaCadastro.this,
                                        "Erro ao cadastrar",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                btnCadastrar.setEnabled(true);
                btnCadastrar.setText("Cadastrar");
                Toast.makeText(TelaCadastro.this,
                        "Erro ao criar perfil: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            });
        }
    }

    private void limparCampos() {
        edtNome.setText("");
        edtNickname.setText("");
        edtSenhaCadastro.setText("");
        edtConfirmarSenha.setText("");
        edtNome.requestFocus();
    }
}
