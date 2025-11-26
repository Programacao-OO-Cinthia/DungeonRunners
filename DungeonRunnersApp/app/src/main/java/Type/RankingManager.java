package Type;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class RankingManager {
    private static final int TOP_COUNT = 10;
    private List<RankingPlayer> rankingGeral;
    private SupabaseClient supabaseClient;

    public RankingManager() {
        this.rankingGeral = new ArrayList<>();
        this.supabaseClient = new SupabaseClient();
    }

    /**
     * Carrega o Top 10 jogadores do ranking
     */
    public void carregarRankingGeral(RankingCallback callback) {        // buscar top 10 jogadores ordenados por kmTotal

        String query = "perfis?select=id,nickname,nivel,kmTotal&order=kmTotal.desc&limit=" + TOP_COUNT;

        supabaseClient.request("GET", query, null, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                callback.onRankingError("Erro de conexão: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                String responseBody = response.body().string();

                try {
                    if (response.isSuccessful()) {
                        JSONArray jogadoresArray = new JSONArray(responseBody);
                        List<RankingPlayer> ranking = processarRanking(jogadoresArray);
                        callback.onRankingCarregado(ranking);
                    } else {
                        callback.onRankingError("Erro ao carregar ranking: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onRankingError("Erro ao processar ranking: " + e.getMessage());
                }
            }
        });
    }

    private List<RankingPlayer> processarRanking(JSONArray jogadoresArray) throws Exception { // processa o json do resultado e cria uma lista ordenada
        List<RankingPlayer> ranking = new ArrayList<>();

        for (int i = 0; i < jogadoresArray.length(); i++) {
            JSONObject jogadorJson = jogadoresArray.getJSONObject(i);

            String id = jogadorJson.getString("id");
            String nickname = jogadorJson.getString("nickname");
            int nivel = jogadorJson.getInt("nivel");
            double kmTotal = jogadorJson.getDouble("kmTotal");

            RankingPlayer player = new RankingPlayer(id, nickname, nivel, kmTotal);
            ranking.add(player);
        }

        Collections.sort(ranking);

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setPosicao(i + 1);
        }

        return ranking;
    }
    
    public void buscarPosicaoJogador(String userId, PosicaoCallback callback) { // Busca a posição atual do jogador no ranking geral
        String query = "perfis?select=id,kmTotal&order=kmTotal.desc";

        supabaseClient.request("GET", query, null, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                callback.onPosicaoError("Erro ao buscar posição: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                String responseBody = response.body().string();

                try {
                    if (response.isSuccessful()) {
                        JSONArray todosJogadores = new JSONArray(responseBody);
                        int posicao = calcularPosicaoJogador(userId, todosJogadores);
                        callback.onPosicaoEncontrada(posicao);
                    } else {
                        callback.onPosicaoError("Erro ao buscar posição");
                    }
                } catch (Exception e) {
                    callback.onPosicaoError("Erro ao processar posição: " + e.getMessage());
                }
            }
        });
    }

    private int calcularPosicaoJogador(String userId, JSONArray todosJogadores) throws Exception {
        for (int i = 0; i < todosJogadores.length(); i++) {
            JSONObject jogador = todosJogadores.getJSONObject(i);
            if (jogador.getString("id").equals(userId)) {
                return i + 1; // Posição no ranking 
            }
        }
        return -1; 
    }

    // Interfaces de callback para tratamento assíncrono
    public interface RankingCallback {
        void onRankingCarregado(List<RankingPlayer> ranking);
        void onRankingError(String erro);
    }

    public interface PosicaoCallback {
        void onPosicaoEncontrada(int posicao);
        void onPosicaoError(String erro);
    }
}
