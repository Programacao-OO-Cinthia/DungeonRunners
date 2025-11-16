package Type;

public class RankingPlayer implements Comparable<RankingPlayer> {
    private String id;
    private String nickname;
    private int nivel;
    private double kmTotal;
    private int posicao;

    public RankingPlayer(String id, String nickname, int nivel, double kmTotal) {
        this.id = id;
        this.nickname = nickname;
        this.nivel = nivel;
        this.kmTotal = kmTotal;
    }

    // Getters e Setters
    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public int getNivel() { return nivel; }
    public double getKmTotal() { return kmTotal; }
    public int getPosicao() { return posicao; }
    public void setPosicao(int posicao) { this.posicao = posicao; }

    @Override
    public int compareTo(RankingPlayer outro) {
        // Ordena por KM total (decrescente)
        return Double.compare(outro.getKmTotal(), this.getKmTotal());
    }

    @Override
    public String toString() {
        return String.format("#%d %s - Nv.%d - %.2f km",
                posicao, nickname, nivel, kmTotal);
    }
}