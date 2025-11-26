package Type;

public class Cla {
    private int id;
    private String nome;
    private String descricao;
    private int membrosCount;
    private int pontuacao;
    private String dataCriacao;
    private String liderNome; //campo para nome do líder

    public Cla(int id, String nome, String descricao, int membrosCount, int pontuacao, String dataCriacao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.membrosCount = membrosCount;
        this.pontuacao = pontuacao;
        this.dataCriacao = dataCriacao;
        this.liderNome = "Líder"; // Valor padrão
    }

    // Construtor com líder
    public Cla(int id, String nome, String descricao, int membrosCount, int pontuacao, String dataCriacao, String liderNome) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.membrosCount = membrosCount;
        this.pontuacao = pontuacao;
        this.dataCriacao = dataCriacao;
        this.liderNome = liderNome;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public int getMembrosCount() { return membrosCount; }
    public int getPontuacao() { return pontuacao; }
    public String getDataCriacao() { return dataCriacao; }
    public String getLiderNome() { return liderNome; } 

    public double getKmTotal() { return pontuacao; }
}
