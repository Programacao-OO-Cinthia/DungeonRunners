package Type;

public class MembroGuilda extends User{
    private String id;
    private String nickname;
    private int nivel;
    private double kmTotal;
    private int xp;
    private String cargo;

    public MembroGuilda(String id, String nickname, int nivel, int idGuilda, double kmTotal, int xp, String cargo) {
        super(id, nickname, nivel, idGuilda, kmTotal);
        this.id = id;
        this.nickname = nickname;
        this.nivel = nivel;
        this.kmTotal = kmTotal;
        this.xp = xp;
        this.cargo = cargo;
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public int getNivel() { return nivel; }
    public double getKmTotal() { return kmTotal; }
    public int getXp() { return xp; }
    public String getCargo() { return cargo; }
}
