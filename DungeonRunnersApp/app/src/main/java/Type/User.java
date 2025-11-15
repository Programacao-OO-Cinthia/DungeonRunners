package Type;

public class User {
    private String id,nickname;
    private int nivel,idCla;
    private double kmTotal;

    public User(String id, String nickname,int nivel, int idCla, double kmTotal){
        this.id = id;
        this.idCla = idCla;
        this.nivel = nivel;
        this.kmTotal = kmTotal;
        this.nickname = nickname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public int getIdCla() {
        return idCla;
    }

    public void setIdCla(int idCla) {
        this.idCla = idCla;
    }

    public double getKmTotal() {
        return kmTotal;
    }

    public void setKmTotal(double kmTotal) {
        this.kmTotal = kmTotal;
    }
}
