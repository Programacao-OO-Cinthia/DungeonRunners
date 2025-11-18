package com.example.dungeonrunnersapp;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
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

import org.json.JSONArray;
import org.json.JSONObject;

import Type.SupabaseClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.util.List;

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
    private Handler handler = new Handler();

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

        // Criar canal de notificação
        criarCanalNotificacao();

        // Configurar callback de localização
        configurarLocationCallback();

        // Iniciar como serviço foreground
        startForeground(NOTIFICATION_ID, criarNotificacao());

        // Iniciar atualizações de localização
        iniciarAtualizacoesLocalizacao();

        // Sincronizar com o banco de dados
        sincronizarDadosIniciais();

    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dungeon Runners Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Rastreamento de localização para Dungeon Runners");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

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
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dungeon Runners")
                .setContentText(String.format("Distância percorrida: %.2f km", distanciaPercorrida / 1000))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void atualizarNotificacao(double distancia) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dungeon Runners")
                .setContentText(String.format("Distância percorrida: %.2f km", distancia / 1000))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
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
        if (ultimaLocalizacao != null) {
            float distancia = ultimaLocalizacao.distanceTo(location);

            // Filtros mais precisos para background
            boolean precisaValida = location.hasAccuracy() && location.getAccuracy() < 20;
            boolean distanciaValida = distancia > 3 && distancia < 200; // Range mais flexível para background
            boolean tempoValido = (location.getTime() - ultimaLocalizacao.getTime()) < 30000; // Máximo 30 segundos entre pontos

            if (precisaValida && distanciaValida && tempoValido) {
                distanciaPercorrida += distancia;

                // Atualizar notificação
                atualizarNotificacao(distanciaPercorrida);

                // Salvar no SharedPreferences
                salvarProgresso();

                // Enviar broadcast para atualizar a UI
                enviarBroadcast();

                // Atualizar no banco de dados
                atualizarBancoDados();

                // Calcular XP (1 XP a cada 100 metros)
                int xpGanho = (int) (distancia / 100);
                if (xpGanho > 0) {
                    adicionarXP(xpGanho);
                }
            } else {
            }
        } else {
        }

        ultimaLocalizacao = location;
    }

    private void enviarBroadcast() {
        try {
            Intent intent = new Intent(ACTION_KM_ATUALIZADO);
            intent.putExtra("kmTotal", (float) (distanciaPercorrida / 1000));
            intent.putExtra("timestamp", System.currentTimeMillis());

            sendBroadcast(intent);

        } catch (Exception e) {
        }
    }

    private void sincronizarDadosIniciais() {
        String userId = prefs.getString("userId", "");
        if (!userId.isEmpty()) {
            buscarDadosAtualizados(userId);
        }
    }

    private void buscarDadosAtualizados(String userId) {
        String query = "perfis?select=kmTotal,xp,fitcoins&id=eq." + userId;

        supabaseClient.request("GET", query, null, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray array = new JSONArray(responseBody);
                        if (array.length() > 0) {
                            JSONObject data = array.getJSONObject(0);

                            SharedPreferences.Editor editor = prefs.edit();
                            if (data.has("kmTotal") && !data.isNull("kmTotal")) {
                                double kmBanco = data.getDouble("kmTotal");
                                editor.putFloat("kmPercorridos", (float) kmBanco);
                                distanciaPercorrida = kmBanco * 1000;
                            }
                            if (data.has("xp") && !data.isNull("xp")) {
                                editor.putInt("xp", data.getInt("xp"));
                            }
                            if (data.has("fitcoins") && !data.isNull("fitcoins")) {
                                editor.putInt("fitcoins", data.getInt("fitcoins"));
                            }
                            editor.apply();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    private void salvarProgresso() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("kmPercorridos", (float) (distanciaPercorrida / 1000));
        editor.apply();
    }

    private void atualizarBancoDados() {
        String userId = prefs.getString("userId", "");
        if (userId.isEmpty()) {
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("kmTotal", distanciaPercorrida / 1000);

            supabaseClient.update("perfis", "id=eq." + userId, data, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful()) {
                    } else {
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "sem corpo";
                        } catch (Exception e) {
                        }
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    private void adicionarXP(int xp) {
        int xpAtual = prefs.getInt("xp", 0);
        int novoXP = xpAtual + xp;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("xp", novoXP);
        editor.apply();

        String userId = prefs.getString("userId", "");
        if (!userId.isEmpty()) {
            try {
                JSONObject data = new JSONObject();
                data.put("xp", novoXP);

                supabaseClient.update("perfis", "id=eq." + userId, data, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        if (response.isSuccessful()) {
                        }
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private boolean verificarPermissoes() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Para Android 10+, verificar permissão de background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) { // se não tiver a permissão ele funciona mas sem contar o segundo plano
            }
        }
        return true;
    }

    private boolean isAppEmBackground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return true;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return true;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(getPackageName())) {
                return appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return true;
    }

    private void iniciarAtualizacoesLocalizacao() {
        if (!verificarPermissoes()) {
            return;
        }

        // configuração melhorada para background
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                isAppEmBackground() ? 15000 : 8000) // 15s em background, 8s em foreground
                .setMinUpdateIntervalMillis(isAppEmBackground() ? 10000 : 5000)
                .setMaxUpdateDelayMillis(30000)
                .setWaitForAccurateLocation(true)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                getMainLooper()
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // garantir que as atualizações continuem mesmo após reinício
        handler.postDelayed(() -> {
            if (fusedLocationClient != null && locationCallback != null) {
                iniciarAtualizacoesLocalizacao();
            }
        }, 1000);

        return START_STICKY; // faz o sistema reiniciar o serviço se for morto
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy(); // destroi o serviço

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent); // remove a tarefa
    }
}