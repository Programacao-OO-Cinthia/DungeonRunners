package Type;
public class Player extends User{
    private int fitCoins, xp;
    public Player(String id, String nickname, int nivel, int idCla,double kmTotal, int fitCoins, int xp ){
        super(id,nickname,nivel,idCla,kmTotal);
        this.fitCoins = fitCoins;
        this.xp = xp;
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
}
