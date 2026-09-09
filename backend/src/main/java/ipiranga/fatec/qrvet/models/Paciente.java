package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "paciente")
public class Paciente {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Tutor tutor;
        private String nome;
        private String especie;
        private String raca;
        private String sexo;
        private LocalDate dataNascimento;
        private BigDecimal peso;
        private String observacoes;

        public Builder tutor(Tutor tutor) { this.tutor = tutor; return this; }
        public Builder nome(String nome) { this.nome = nome; return this; }
        public Builder especie(String especie) { this.especie = especie; return this; }
        public Builder raca(String raca) { this.raca = raca; return this; }
        public Builder sexo(String sexo) { this.sexo = sexo; return this; }
        public Builder dataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; return this; }
        public Builder peso(BigDecimal peso) { this.peso = peso; return this; }
        public Builder observacoes(String observacoes) { this.observacoes = observacoes; return this; }

        public Paciente build() {
            Paciente paciente = new Paciente();
            paciente.tutor = tutor;
            paciente.nome = nome;
            paciente.especie = especie;
            paciente.raca = raca;
            paciente.sexo = sexo;
            paciente.dataNascimento = dataNascimento;
            paciente.peso = peso;
            paciente.observacoes = observacoes;
            return paciente;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;
    @Column(name = "especie", nullable = false, length = 50)
    private String especie;
    @Column(name = "raca", nullable = false, length = 50)
    private String raca;
    @Column(name = "sexo", nullable = false, length = 20)
    private String sexo;
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
    @Column(name = "peso", nullable = false, precision = 6, scale = 2)
    private BigDecimal peso;
    @Column(name = "observacoes")
    private String observacoes;
    @Column(name = "data_cadastro", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataCadastro;

    @PrePersist
    void initializeDataCadastro() { dataCadastro = Instant.now(); }

    public Long getId() { return id; }
    public Tutor getTutor() { return tutor; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getRaca() { return raca; }
    public void setRaca(String raca) { this.raca = raca; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Instant getDataCadastro() { return dataCadastro; }
}
