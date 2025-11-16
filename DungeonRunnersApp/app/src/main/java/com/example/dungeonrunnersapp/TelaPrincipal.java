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
import android.util.Log;
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

    // Player atual
    private Player playerAtual;

    // UI Components
    private TextView txtNomeUsuario;
    private TextView txtNivelUsuario;
    private TextView txtXP;
    private TextView txtFitCoins;
    private TextView txtKmPercorridos;
    private FloatingActionButton fabLocalizacao;

    // Menu Buttons
    private ImageButton btnMissoes;
    private ImageButton btnInventario;
    private ImageButton btnGuilda;
    private ImageButton btnLoja;

    // Dungeon Cards
    private CardView cardDungeon1;
    private CardView cardDungeon2;
    private CardView cardDungeon3;

    // Mapa e Localização
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Launcher para permissões
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private boolean permissoesLocalizacaoConcedidas = false;

    // BroadcastReceiver para atualizar KMs
    private BroadcastReceiver kmReceiver;
    private boolean isReceiverRegistered = false;

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

        Log.d(TAG, "onCreate iniciado");

        inicializarPermissionLauncher();
        inicializarComponentes();
        carregarPlayer();
        atualizarUICompleta();
        configurarListeners();
        inicializarMapa();
        verificarPermissoesLocalizacao();
        configurarNavegacaoInferior();

        // Inicializar e registrar o BroadcastReceiver
        inicializarBroadcastReceiver();
        registrarBroadcastReceiver();
    }

    private void inicializarBroadcastReceiver() {
        kmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "📡 BroadcastReceiver triggered");

                if (intent.getAction() != null && intent.getAction().equals(ServicoLocalizacao.ACTION_KM_ATUALIZADO)) {
                    float kmTotal = intent.getFloatExtra("kmTotal", 0.0f);
                    long timestamp = intent.getLongExtra("timestamp", 0);

                    Log.d(TAG, "🎯 KM recebido do broadcast: " + kmTotal + " km");

                    // Executar na thread UI
                    runOnUiThread(() -> {
                        // Atualizar o objeto Player
                        if (playerAtual != null) {
                            playerAtual.setKmTotal(kmTotal);
                        }

                        // Atualizar UI
                        txtKmPercorridos.setText(String.format("%.2f km", kmTotal));

                        // Atualizar também XP se necessário
                        atualizarUICompleta();

                        Log.d(TAG, "✅ UI atualizada em tempo real - KM: " + kmTotal);
                    });

                    // Atualizar SharedPreferences
                    salvarPlayer();
                }
            }
        };
    }

    private void registrarBroadcastReceiver() {
        try {
            IntentFilter filter = new IntentFilter(ServicoLocalizacao.ACTION_KM_ATUALIZADO);

            // CORREÇÃO: Especificar flag RECEIVER_EXPORTED apropriadamente
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ - Precisa especificar explicitamente
                    registerReceiver(kmReceiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    // Android 8-12 - Não precisa da flag
                    registerReceiver(kmReceiver, filter,Context.RECEIVER_NOT_EXPORTED);
                }
            }

            isReceiverRegistered = true;
            Log.d(TAG, "✅ BroadcastReceiver registrado com sucesso!");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro ao registrar BroadcastReceiver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocation != null && fineLocation) {
                        permissoesLocalizacaoConcedidas = true;
                        inicializarLocalizacao();
                        habilitarLocalizacaoNoMapa();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            solicitarPermissaoBackground();
                        } else {
                            iniciarServicoLocalizacao();
                        }
                    } else if (coarseLocation != null && coarseLocation) {
                        permissoesLocalizacaoConcedidas = true;
                        inicializarLocalizacao();
                        Toast.makeText(this, "Localização aproximada ativada", Toast.LENGTH_SHORT).show();
                    } else {
                        permissoesLocalizacaoConcedidas = false;
                        mostrarDialogoPermissaoNegada();
                    }
                }
        );
    }

    private void inicializarComponentes() {
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
        SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);

        String id = prefs.getString("userId", "");
        String nickname = prefs.getString("nickname", "Runner");
        int nivel = prefs.getInt("nivel", 1);
        int idCla = prefs.getInt("idCla", 0);
        double kmTotal = prefs.getFloat("kmPercorridos", 0.0f);
        int fitCoins = prefs.getInt("fitcoins", 100);
        int xp = prefs.getInt("xp", 0);

        // Criar objeto Player
        playerAtual = new Player(id, nickname, nivel, idCla, kmTotal, fitCoins, xp);

        Log.d(TAG, "Player carregado: " + nickname + " - KM: " + kmTotal);
    }

    private void atualizarUICompleta() {
        if (playerAtual != null) {
            txtNomeUsuario.setText(playerAtual.getNickname());
            txtNivelUsuario.setText("Nível " + playerAtual.getNivel());
            txtXP.setText(String.valueOf(playerAtual.getXp()));
            txtFitCoins.setText(String.valueOf(playerAtual.getFitCoins()));
            txtKmPercorridos.setText(String.format("%.2f km", playerAtual.getKmTotal()));

            Log.d(TAG, "UI atualizada - KM: " + playerAtual.getKmTotal());
        }
    }

    private void configurarListeners() {
        fabLocalizacao.setOnClickListener(v -> centralizarLocalizacao());

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
            permissoesLocalizacaoConcedidas = true;
            inicializarLocalizacao();
            habilitarLocalizacaoNoMapa();

            // Verificar se o serviço já está rodando
            if (!isServicoLocalizacaoRodando()) {
                iniciarServicoLocalizacao();
            }
        } else {
            solicitarPermissoesLocalizacao();
        }
    }

    private void solicitarPermissoesLocalizacao() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissão de Localização")
                    .setMessage("O Dungeon Runners precisa acessar sua localização para funcionar corretamente.")
                    .setPositiveButton("Permitir", (dialog, which) -> {
                        locationPermissionLauncher.launch(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        });
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> mostrarDialogoPermissaoNegada())
                    .show();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void solicitarPermissaoBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Localização em Segundo Plano")
                        .setMessage("Para rastrear sua movimentação em segundo plano, escolha 'Permitir o tempo todo'.")
                        .setPositiveButton("Continuar", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 100);
                        })
                        .setNegativeButton("Agora não", (dialog, which) -> {
                            Toast.makeText(this, "Rastreamento em segundo plano desativado", Toast.LENGTH_LONG).show();
                        })
                        .show();
            } else {
                iniciarServicoLocalizacao();
            }
        }
    }

    private void mostrarDialogoPermissaoNegada() {
        new AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("O app não funcionará sem acesso à localização.")
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

        iniciarAtualizacoesLocalizacao();
        obterLocalizacaoAtual();
    }

    private void iniciarAtualizacoesLocalizacao() {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Log.d(TAG, "Serviço de localização iniciado");
            Toast.makeText(this, "Rastreamento iniciado!", Toast.LENGTH_SHORT).show();
        }
    }

    private void iniciarOuPararServicoLocalizacao(boolean iniciar) {
        Intent serviceIntent = new Intent(this, ServicoLocalizacao.class);

        if (iniciar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Serviço de localização iniciado");
        } else {
            stopService(serviceIntent);
            Log.d(TAG, "Serviço de localização parado");
        }
    }

    private boolean isServicoLocalizacaoRodando() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

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
                    Toast.makeText(this, "Erro ao obter localização", Toast.LENGTH_SHORT).show();
                });
    }

    private void atualizarPosicaoMapa(Location location) {
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
        Log.d(TAG, "onResume chamado");

        // Verificar se o receiver não está registrado e registrar novamente
        if (!isReceiverRegistered) {
            registrarBroadcastReceiver();
        }

        if (permissoesLocalizacaoConcedidas) {
            if (!isServicoLocalizacaoRodando()) {
                iniciarServicoLocalizacao();
            }
            if (fusedLocationClient != null && locationCallback != null) {
                iniciarAtualizacoesLocalizacao();
            }
        }
        carregarPlayer();
        atualizarUICompleta();

        // Forçar uma atualização inicial
        atualizarDadosDoServico();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause chamado");

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // NÃO desregistrar o receiver aqui para manter funcionando em background
        salvarPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy chamado");

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Desregistrar o receiver apenas no onDestroy
        if (isReceiverRegistered && kmReceiver != null) {
            try {
                unregisterReceiver(kmReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "✅ BroadcastReceiver desregistrado");
            } catch (Exception e) {
                Log.e(TAG, "❌ Erro ao desregistrar receiver: " + e.getMessage());
            }
        }
    }

    // Método para forçar atualização dos dados do serviço
    private void atualizarDadosDoServico() {
        SharedPreferences prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);
        float kmAtual = prefs.getFloat("kmPercorridos", 0.0f);

        if (playerAtual != null) {
            playerAtual.setKmTotal(kmAtual);
        }

        txtKmPercorridos.setText(String.format("%.2f km", kmAtual));
        Log.d(TAG, "📊 Dados atualizados do SharedPreferences: " + kmAtual + " km");
    }

    private void salvarPlayer() {
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
            Log.d(TAG, "Player salvo - KM: " + playerAtual.getKmTotal());
        }
    }

    private void configurarNavegacaoInferior() {
        findViewById(R.id.btnNavRanking).setOnClickListener(v -> {
            Intent intent = new Intent(TelaPrincipal.this, TelaRanking.class);
            startActivity(intent);
        });
    }
}