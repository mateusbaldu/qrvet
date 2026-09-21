package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tutor")
public class Tutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 11, unique = true)
    private String cpf;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "endereco", nullable = false, length = 255)
    private String endereco;

    @Column(name = "data_cadastro", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataCadastro;

    @PrePersist
    void initializeDataCadastro() {
        dataCadastro = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Instant getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Instant dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String nome;
        private String cpf;
        private String telefone;
        private String email;
        private String endereco;

        public Builder nome(String nome) { this.nome = nome; return this; }
        public Builder cpf(String cpf) { this.cpf = cpf; return this; }
        public Builder telefone(String telefone) { this.telefone = telefone; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder endereco(String endereco) { this.endereco = endereco; return this; }

        public Tutor build() {
            Tutor tutor = new Tutor();
            tutor.nome = nome;
            tutor.cpf = cpf;
            tutor.telefone = telefone;
            tutor.email = email;
            tutor.endereco = endereco;
            return tutor;
        }
    }

}
