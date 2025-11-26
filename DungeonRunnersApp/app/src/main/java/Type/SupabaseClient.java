package Type;

import okhttp3.*;
import org.json.JSONObject;
import java.util.UUID;

/**
 * Classe responsável pela conexão com o banco de dados Supabase
 * e pela execução de requisições HTTP
 */
public class SupabaseClient {
    private static final String SUPABASE_URL = "https://gcttbfouxzdgmsypdcyz.supabase.co/rest/v1/";
    private static final String SUPABASE_KEY = "sb_publishable_gVu2gzS2lvFCTi_k1Idbjg_gkILLvU6";

    private final OkHttpClient client = new OkHttpClient();


     //Envia uma requisição HTTP genérica para o Supabase.

    public void request(String method, String table, JSONObject body, Callback callback) {
        RequestBody requestBody = body != null
                ? RequestBody.create(body.toString(), MediaType.parse("application/json"))
                : null;

        Request request = new Request.Builder()
                .url(SUPABASE_URL + table)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .method(method, requestBody)
                .build();

        client.newCall(request).enqueue(callback);
    }

    //Faz uma requisição GET simples para buscar todos os registros de uma tabela.
    public void getAll(String table, Callback callback) {
        request("GET", table + "?select=*", null, callback);
    }

    // Envia um novo registro para a tabela especificada.
    public void insert(String table, JSONObject data, Callback callback) {
        request("POST", table, data, callback);
    }

    //Atualiza um registro existente com base em uma condição
    public void update(String table, String condition, JSONObject data, Callback callback) {
        request("PATCH", table + "?" + condition, data, callback);
    }

    // Deleta um registro com base em uma condição
    public void delete(String table, String condition, Callback callback) {
        request("DELETE", table + "?" + condition, null, callback);
    }

    //Insere um novo perfil com UUID gerado automaticamente.
    public void inserirPerfil(String nome, int idade, Callback callback) {
        try {
            String uuid = UUID.randomUUID().toString(); // gera o UUID

            JSONObject perfil = new JSONObject();
            perfil.put("id", uuid); // envia o UUID manualmente
            perfil.put("nome", nome);
            perfil.put("idade", idade);
            perfil.put("nivel", 1);
            perfil.put("xp", 0);
            perfil.put("fitcoins", 100);
            perfil.put("role", "usuario");

            insert("perfis", perfil, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
