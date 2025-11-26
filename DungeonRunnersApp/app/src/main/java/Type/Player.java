package Type;

public class Player extends User{
    private int fitCoins, xp;
    private String guildaNome; 

    public Player(String id, String nickname, int nivel, int idCla, double kmTotal, int fitCoins, int xp) {
        super(id, nickname, nivel, idCla, kmTotal);
        this.fitCoins = fitCoins;
        this.xp = xp;
    }

    public Player(String id, String nickname, int nivel, int idCla, double kmTotal, int fitCoins, int xp, String guildaNome) {
        super(id, nickname, nivel, idCla, kmTotal);
        this.fitCoins = fitCoins;
        this.xp = xp;
        this.guildaNome = guildaNome;
    }

    public int getFitCoins() {
        return fitCoins;
    }

    public void setFitCoins(int fitCoins) {
        this.fitCoins = fitCoins;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getGuildaNome() {
        return guildaNome;
    }

    public void setGuildaNome(String guildaNome) {
        this.guildaNome = guildaNome;
    }

    //  Usa o idCla da classe User
    public boolean temGuilda() {
        return getIdCla() > 0; // idCla é int, verifica se é maior que 0
    }

    // Getter para o ID do clã
    public int getGuildaId() {
        return getIdCla();
    }
}
