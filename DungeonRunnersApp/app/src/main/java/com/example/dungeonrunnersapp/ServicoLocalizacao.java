package com.example.dungeonrunnersapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONObject;

import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ServicoLocalizacao extends Service {

    private static final String TAG = "ServicoLocalizacao";
    private static final String CHANNEL_ID = "DungeonRunnersChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_KM_ATUALIZADO = "com.example.dungeonrunnersapp.KM_ATUALIZADO";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double distanciaPercorrida = 0.0;
    private Location ultimaLocalizacao = null;
    private SupabaseClient supabaseClient;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        supabaseClient = new SupabaseClient();
        prefs = getSharedPreferences("DungeonRunners", MODE_PRIVATE);

        // Carregar distância já percorrida
        distanciaPercorrida = prefs.getFloat("kmPercorridos", 0.0f) * 1000; // Converter km para metros

        // DIAGNÓSTICO: Verificar userId
        String userId = prefs.getString("userId", "");
        Log.d(TAG, "=== SERVIÇO INICIADO ===");
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "Distância inicial: " + (distanciaPercorrida / 1000) + " km");

        // Criar canal de notificação
        criarCanalNotificacao();

        // Configurar callback de localização
        configurarLocationCallback();

        // Iniciar como serviço foreground
        startForeground(NOTIFICATION_ID, criarNotificacao());

        // Iniciar atualizações de localização
        iniciarAtualizacoesLocalizacao();

        Log.d(TAG, "Serviço configurado e pronto");
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dungeon Runners Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Rastreamento de localização para Dungeon Runners");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification criarNotificacao() {
        Intent notificationIntent = new Intent(this, TelaPrincipal.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dungeon Runners")
                .setContentText(String.format("Distância percorrida: %.2f km", distanciaPercorrida / 1000))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void atualizarNotificacao(double distancia) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dungeon Runners")
                .setContentText(String.format("Distância percorrida: %.2f km", distancia / 1000))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void configurarLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    processarLocalizacao(location);
                }
            }
        };
    }

    private void processarLocalizacao(Location location) {
        Log.d(TAG, "Nova localização recebida: " + location.getLatitude() + ", " + location.getLongitude());

        if (ultimaLocalizacao != null) {
            float distancia = ultimaLocalizacao.distanceTo(location);

            Log.d(TAG, "Distância calculada: " + distancia + " metros");

            // Só conta se a distância for maior que 5 metros e menor que 100 (evita GPS drift)
            if (distancia > 5 && distancia < 100) {
                distanciaPercorrida += distancia;

                Log.d(TAG, "Distância percorrida: " + distancia + "m. Total: " + (distanciaPercorrida / 1000) + "km");

                // Atualizar notificação
                atualizarNotificacao(distanciaPercorrida);

                // Salvar no SharedPreferences
                salvarProgresso();

                // Enviar broadcast para atualizar a UI - AGORA COM MAIS INFORMAÇÕES
                enviarBroadcast();

                // Atualizar no banco de dados
                atualizarBancoDados();

                // Calcular XP (1 XP a cada 100 metros)
                int xpGanho = (int) (distancia / 100);
                if (xpGanho > 0) {
                    adicionarXP(xpGanho);
                }
            } else {
                Log.d(TAG, "Distância ignorada: " + distancia + "m (fora do range 5-100m)");
            }
        } else {
            Log.d(TAG, "Primeira localização recebida - definindo como referência");
        }

        ultimaLocalizacao = location;
    }

    private void enviarBroadcast() {
        try {
            Intent intent = new Intent(ACTION_KM_ATUALIZADO);
            intent.putExtra("kmTotal", (float) (distanciaPercorrida / 1000));
            intent.putExtra("timestamp", System.currentTimeMillis()); // Para evitar duplicatas

            // Enviar broadcast de forma explícita
            sendBroadcast(intent);

            Log.d(TAG, "📡 Broadcast enviado: " + (distanciaPercorrida / 1000) + " km");
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro ao enviar broadcast: " + e.getMessage());
        }
    }

    private void salvarProgresso() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("kmPercorridos", (float) (distanciaPercorrida / 1000));
        editor.apply();

        Log.d(TAG, "Progresso salvo no SharedPreferences: " + (distanciaPercorrida / 1000) + " km");
    }

    private void atualizarBancoDados() {
        String userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            Log.e(TAG, "❌ userId não encontrado. Não foi possível atualizar o banco.");
            return;
        }

        try {
            JSONObject data = new JSONObject();
            // CORREÇÃO: Usar "kmTotal" (com T maiúsculo) em vez de "kmtotal"
            data.put("kmTotal", distanciaPercorrida / 1000);

            Log.d(TAG, "📤 Atualizando banco...");
            Log.d(TAG, "   User ID: " + userId);
            Log.d(TAG, "   KM Total: " + (distanciaPercorrida / 1000));

            supabaseClient.update("perfis", "id=eq." + userId, data, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                    Log.e(TAG, "❌ Erro ao atualizar banco de dados: " + e.getMessage());
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Banco de dados atualizado com sucesso!");

                        // Atualizar SharedPreferences também
                        salvarProgresso();
                    } else {
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "sem corpo";
                            Log.e(TAG, "❌ Erro ao atualizar banco: " + response.code() + " - " + response.message());
                            Log.e(TAG, "   Body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Erro ao ler resposta: " + e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro ao criar JSON para atualização: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void adicionarXP(int xp) {
        int xpAtual = prefs.getInt("xp", 0);
        int novoXP = xpAtual + xp;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("xp", novoXP);
        editor.apply();

        // Atualizar no banco
        String userId = prefs.getString("userId", "");
        if (!userId.isEmpty()) {
            try {
                JSONObject data = new JSONObject();
                data.put("xp", novoXP);

                supabaseClient.update("perfis", "id=eq." + userId, data, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                        Log.e(TAG, "Erro ao atualizar XP: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "XP atualizado: +" + xp + " (Total: " + novoXP + ")");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro ao atualizar XP: " + e.getMessage());
            }
        }
    }

    private boolean verificarPermissoes() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissões de localização não concedidas");
            return false;
        }
        return true;
    }

    private void iniciarAtualizacoesLocalizacao() {
        if (!verificarPermissoes()) {
            Log.e(TAG, "Permissões insuficientes para iniciar atualizações de localização");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000) // Atualiza a cada 5 segundos
                .setMinUpdateIntervalMillis(3000) // Mínimo de 3 segundos
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
        Log.d(TAG, "Atualizações de localização iniciadas");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand chamado");
        return START_STICKY; // Reinicia o serviço se for morto pelo sistema
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "Serviço destruído");
    }
}