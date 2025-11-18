package com.example.dungeonrunnersapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import Type.Player;

public class TelaPrincipal extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TelaPrincipal";

    // Jogador atual
    private Player playerAtual;

    // Componentes da interface
    private TextView txtNomeUsuario;
    private TextView txtNivelUsuario;
    private TextView txtXP;
    private TextView txtFitCoins;
    private TextView txtKmPercorridos;
    private FloatingActionButton fabLocalizacao;

    // Botões do menu
    private ImageButton btnMissoes;
    private ImageButton btnInventario;
    private ImageButton btnGuilda;
    private ImageButton btnLoja;

    // Cards das dungeons
    private CardView cardDungeon1;
    private CardView cardDungeon2;
    private CardView cardDungeon3;

    // Sistema de mapa e localização
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Gerenciamento de permissões
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> backgroundLocationLauncher;
    private boolean permissoesLocalizacaoConcedidas = false;

    // Receptor de broadcasts para atualizar os quilômetros em tempo real
    private BroadcastReceiver kmReceiver;
    private boolean isReceiverRegistered = false;
    private Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_principal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa tudo que precisa pra tela funcionar
        inicializarPermissionLaunchers();
        inicializarComponentes();
        carregarPlayer();
        atualizarUICompleta();
        configurarListeners();
        inicializarMapa();
        verificarPermissoesLocalizacao();
        configurarNavegacaoInferior();

        // Configura o sistema de receber atualizações de distância
        inicializarBroadcastReceiver();
        registrarBroadcastReceiver();

        // Aguarda um pouco e força o início do rastreamento
        uiHandler.postDelayed(() -> {
            if (permissoesLocalizacaoConcedidas && !isServicoLocalizacaoRodando()) {
                iniciarServicoLocalizacao();
            }
        }, 2000);
    }

    private void inicializarPermissionLaunchers() {
        // Configura o tratamento de resposta das permissões de localização
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocation != null && fineLocation) {
                        // Tudo certo! Permissão precisa concedida
                        permissoesLocalizacaoConcedidas = true;
                        inicializarLocalizacao();
                        habilitarLocalizacaoNoMapa();

                        // Pede permissão de background se o Android for Q ou superior
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            solicitarPermissaoBackground();
                        } else {
                            iniciarServicoLocalizacao();
                        }
                    } else if (coarseLocation != null && coarseLocation) {
                        // Permissão aproximada também serve
                        permissoesLocalizacaoConcedidas = true;
                        inicializarLocalizacao();
                        Toast.makeText(this, "Localização aproximada ativada", Toast.LENGTH_SHORT).show();
                        iniciarServicoLocalizacao();
                    } else {
                        // Usuário negou as permissões
                        permissoesLocalizacaoConcedidas = false;
                        mostrarDialogoPermissaoNegada();
                    }
                }
        );

        // Tratamento específico para permissão de background (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Toast.makeText(this, "Rastreamento em background ativado!", Toast.LENGTH_SHORT).show();
                            iniciarServicoLocalizacao();
                        } else {
                            Toast.makeText(this, "Rastreamento em background limitado", Toast.LENGTH_LONG).show();
                            // Inicia mesmo assim, vai funcionar quando o app tiver aberto
                            iniciarServicoLocalizacao();
                        }
                    }
            );
        }
    }

    private void inicializarBroadcastReceiver() {
        // Cria o receptor que vai escutar atualizações do serviço de localização
        kmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(ServicoLocalizacao.ACTION_KM_ATUALIZADO)) {
                    float kmTotal = intent.getFloatExtra("kmTotal", 0.0f);
                    long timestamp = intent.getLongExtra("timestamp", 0);

                    // Atualiza a interface na thread principal
                    runOnUiThread(() -> {
                        if (playerAtual != null) {
                            playerAtual.setKmTotal(kmTotal);
                        }

                        txtKmPercorridos.setText(String.format("%.2f km", kmTotal));
                        atualizarUICompleta();
                    });

                    salvarPlayer();
                }
            }
        };
    }

    private void registrarBroadcastReceiver() {
        try {
            IntentFilter filter = new IntentFilter(ServicoLocalizacao.ACTION_KM_ATUALIZADO);

            // Registra de forma apropriada dependendo da versão do Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(kmReceiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    registerReceiver(kmReceiver, filter, RECEIVER_EXPORTED);
                }
            } else {
            }

            isReceiverRegistered = true;
        } catch (Exception e) {
            // Se falhar, não tem problema, só não vai atualizar em tempo real
        }
    }

    private void inicializarComponentes() {
        // Pega todas as referências dos componentes visuais
        txtNomeUsuario = findViewById(R.id.txtNomeUsuario);
        txtNivelUsuario = findViewById(R.id.txtNivelUsuario);
        txtXP = findViewById(R.id.txtXP);
        txtFitCoins = findViewById(R.id.txtFitCoins);
        txtKmPercorridos = findViewById(R.id.txtKmPercorridos);
        fabLocalizacao = findViewById(R.id.fabLocalizacao);

        btnMissoes = findViewById(R.id.btnMissoes);
        btnInventario = findViewById(R.id.btnInventario);
        btnGuilda = findViewById(R.id.btnGuilda);
        btnLoja = findViewById(R.id.btnLoja);

        cardDungeon1 = findViewById(R.id.cardDungeon1);
        cardDungeon2 = findViewById(R.id.cardDungeon2);
        cardDungeon3 = findViewById(R.id.cardDungeon3);
    }

    private void carregarPlayer() {
        // Carrega os dados salvos do jogador
        SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);

        String id = prefs.getString("userId", "");
        String nickname = prefs.getString("nickname", "Runner");
        int nivel = prefs.getInt("nivel", 1);
        int idCla = prefs.getInt("idCla", 0);
        double kmTotal = prefs.getFloat("kmPercorridos", 0.0f);
        int fitCoins = prefs.getInt("fitcoins", 100);
        int xp = prefs.getInt("xp", 0);

        playerAtual = new Player(id, nickname, nivel, idCla, kmTotal, fitCoins, xp);
    }

    private void atualizarUICompleta() {
        // Atualiza todos os campos da tela com os dados do jogador
        if (playerAtual != null) {
            txtNomeUsuario.setText(playerAtual.getNickname());
            txtNivelUsuario.setText("Nível " + playerAtual.getNivel());
            txtXP.setText(String.valueOf(playerAtual.getXp()));
            txtFitCoins.setText(String.valueOf(playerAtual.getFitCoins()));
            txtKmPercorridos.setText(String.format("%.2f km", playerAtual.getKmTotal()));
        }
    }

    private void configurarListeners() {
        // Botão de centralizar no mapa
        fabLocalizacao.setOnClickListener(v -> centralizarLocalizacao());

        // Botões do menu (funcionalidades futuras)
        btnMissoes.setOnClickListener(v -> {
            Toast.makeText(this, "Missões em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        btnInventario.setOnClickListener(v -> {
            Toast.makeText(this, "Inventário em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        btnGuilda.setOnClickListener(v -> {
            Toast.makeText(this, "Guilda em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        btnLoja.setOnClickListener(v -> {
            Toast.makeText(this, "Loja em desenvolvimento", Toast.LENGTH_SHORT).show();
        });

        // Cards das dungeons por dificuldade
        cardDungeon1.setOnClickListener(v -> iniciarDungeon("Fácil"));
        cardDungeon2.setOnClickListener(v -> iniciarDungeon("Médio"));
        cardDungeon3.setOnClickListener(v -> iniciarDungeon("Difícil"));
    }

    private void inicializarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void verificarPermissoesLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Já tem permissão, pode iniciar tudo
            permissoesLocalizacaoConcedidas = true;
            inicializarLocalizacao();
            habilitarLocalizacaoNoMapa();

            if (!isServicoLocalizacaoRodando()) {
                iniciarServicoLocalizacao();
            }
        } else {
            // Precisa pedir permissão
            solicitarPermissoesLocalizacao();
        }
    }

    private void solicitarPermissoesLocalizacao() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Mostra explicação antes de pedir
            new AlertDialog.Builder(this)
                    .setTitle("Permissão de Localização")
                    .setMessage("O Dungeon Runners precisa acessar sua localização para rastrear sua distância e progresso no jogo.")
                    .setPositiveButton("Permitir", (dialog, which) -> {
                        locationPermissionLauncher.launch(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        });
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> mostrarDialogoPermissaoNegada())
                    .show();
        } else {
            // Pede diretamente
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void solicitarPermissaoBackground() {
        // Só pra Android 10 ou superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Localização em Segundo Plano")
                        .setMessage("Para rastrear sua distância percorrida mesmo com o app fechado ou em segundo plano, é necessário permitir o acesso à localização em segundo plano.\n\nEscolha 'Permitir o tempo todo' nas próximas telas.")
                        .setPositiveButton("Solicitar", (dialog, which) -> {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                        })
                        .setNegativeButton("Agora não", (dialog, which) -> {
                            Toast.makeText(this, "Rastreamento em segundo plano limitado", Toast.LENGTH_LONG).show();
                            iniciarServicoLocalizacao();
                        })
                        .show();
            } else {
                // Já tem permissão
                iniciarServicoLocalizacao();
            }
        } else {
            iniciarServicoLocalizacao();
        }
    }

    private void mostrarDialogoPermissaoNegada() {
        new AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("Sem acesso à localização, o app não poderá rastrear sua distância percorrida. Você pode ativar a permissão nas configurações.")
                .setPositiveButton("Ir para Configurações", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void inicializarLocalizacao() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configura o callback que vai receber atualizações de localização
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    atualizarPosicaoMapa(location);
                }
            }
        };

        iniciarAtualizacoesLocalizacaoUI();
        obterLocalizacaoAtual();
    }

    private void iniciarAtualizacoesLocalizacaoUI() {
        // Atualizações só para atualizar a interface (a cada 5 segundos)
        // O rastreamento de distância é feito pelo serviço em background
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private void iniciarServicoLocalizacao() {
        if (permissoesLocalizacaoConcedidas) {
            Intent serviceIntent = new Intent(this, ServicoLocalizacao.class);

            try {
                // Inicia como serviço foreground se for Android 8 ou superior
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                // Verifica se o serviço realmente começou a rodar
                uiHandler.postDelayed(() -> {
                    if (isServicoLocalizacaoRodando()) {
                        Toast.makeText(this, "Rastreamento ativo! 🎯", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Erro ao iniciar rastreamento", Toast.LENGTH_SHORT).show();
                    }
                }, 3000);

            } catch (Exception e) {
                Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isServicoLocalizacaoRodando() {
        // Verifica se o serviço está realmente rodando
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServicoLocalizacao.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Aplica estilo escuro no mapa
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Desabilita controles padrão do Google Maps
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

        // Posição inicial padrão (Belo Horizonte)
        LatLng defaultLocation = new LatLng(-19.9167, -43.9345);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        habilitarLocalizacaoNoMapa();
    }

    private void habilitarLocalizacaoNoMapa() {
        if (mMap != null && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            obterLocalizacaoAtual();
        }
    }

    private void obterLocalizacaoAtual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = location;
                        atualizarPosicaoMapa(location);
                    }
                })
                .addOnFailureListener(e -> {
                    // Falhou ao obter localização, mas não precisa fazer nada
                });
    }

    private void atualizarPosicaoMapa(Location location) {
        // Atualiza a câmera do mapa para a posição atual
        if (mMap != null && location != null) {
            LatLng posicao = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicao, 16));
        }
    }

    private void centralizarLocalizacao() {
        if (currentLocation != null) {
            atualizarPosicaoMapa(currentLocation);
        } else {
            Toast.makeText(this, "Obtendo localização...", Toast.LENGTH_SHORT).show();
            obterLocalizacaoAtual();
        }
    }

    private void iniciarDungeon(String dificuldade) {
        Toast.makeText(this, "Iniciando Dungeon " + dificuldade, Toast.LENGTH_SHORT).show();
    }

    public Player getPlayerAtual() {
        return playerAtual;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Tratamento alternativo para permissão de background
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Rastreamento em segundo plano ativado!", Toast.LENGTH_SHORT).show();
                iniciarServicoLocalizacao();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registra o receiver novamente se ele foi desregistrado
        if (!isReceiverRegistered) {
            registrarBroadcastReceiver();
        }

        // Reinicia o serviço se necessário
        if (permissoesLocalizacaoConcedidas) {
            if (!isServicoLocalizacaoRodando()) {
                iniciarServicoLocalizacao();
            }
            if (fusedLocationClient != null && locationCallback != null) {
                iniciarAtualizacoesLocalizacaoUI();
            }
        }

        // Recarrega dados e atualiza a tela
        carregarPlayer();
        atualizarUICompleta();
        atualizarDadosDoServico();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Para as atualizações da UI, mas o serviço continua rodando
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        salvarPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Limpa tudo quando a tela é destruída
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Desregistra o receiver
        if (isReceiverRegistered && kmReceiver != null) {
            try {
                unregisterReceiver(kmReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                // Não conseguiu desregistrar, mas não tem problema
            }
        }

        uiHandler.removeCallbacksAndMessages(null);
    }

    private void atualizarDadosDoServico() {
        // Pega os dados mais recentes salvos pelo serviço
        SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
        float kmAtual = prefs.getFloat("kmPercorridos", 0.0f);

        if (playerAtual != null) {
            playerAtual.setKmTotal(kmAtual);
        }

        txtKmPercorridos.setText(String.format("%.2f km", kmAtual));
    }

    private void salvarPlayer() {
        // Salva todos os dados do jogador
        if (playerAtual != null) {
            SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString("userId", playerAtual.getId());
            editor.putString("nickname", playerAtual.getNickname());
            editor.putInt("nivel", playerAtual.getNivel());
            editor.putInt("idCla", playerAtual.getIdCla());
            editor.putFloat("kmPercorridos", (float) playerAtual.getKmTotal());
            editor.putInt("fitcoins", playerAtual.getFitCoins());
            editor.putInt("xp", playerAtual.getXp());

            editor.apply();
        }
    }

    private void configurarNavegacaoInferior() {
        // Botão de navegação para o ranking
        findViewById(R.id.btnNavRanking).setOnClickListener(v -> {
            Intent intent = new Intent(TelaPrincipal.this, TelaRanking.class);
            startActivity(intent);
        });
    }
}