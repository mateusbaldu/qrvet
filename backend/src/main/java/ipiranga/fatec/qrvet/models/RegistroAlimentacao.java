package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registro_alimentacao")
public class RegistroAlimentacao {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Internacao internacao;
        private User usuario;
        private String alimento;
        private String quantidade;
        private String aceitacaoObservacao;

        public Builder internacao(Internacao internacao) {
            this.internacao = internacao;
            return this;
        }

        public Builder usuario(User usuario) {
            this.usuario = usuario;
            return this;
        }

        public Builder alimento(String alimento) {
            this.alimento = alimento;
            return this;
        }

        public Builder quantidade(String quantidade) {
            this.quantidade = quantidade;
            return this;
        }

        public Builder aceitacaoObservacao(String aceitacaoObservacao) {
            this.aceitacaoObservacao = aceitacaoObservacao;
            return this;
        }

        public RegistroAlimentacao build() {
            RegistroAlimentacao registro = new RegistroAlimentacao();
            registro.internacao = internacao;
            registro.usuario = usuario;
            registro.alimento = alimento;
            registro.quantidade = quantidade;
            registro.aceitacaoObservacao = aceitacaoObservacao;
            return registro;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false, updatable = false)
    private Internacao internacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private User usuario;

    @Column(name = "alimento", nullable = false, length = 100, updatable = false)
    private String alimento;

    @Column(name = "quantidade", nullable = false, length = 50, updatable = false)
    private String quantidade;

    @Column(name = "aceitacao_observacao", columnDefinition = "TEXT", updatable = false)
    private String aceitacaoObservacao;

    @Column(name = "data_hora_registro", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataHoraRegistro;

    protected RegistroAlimentacao() {}

    @PrePersist
    void initializeTimestamp() {
        dataHoraRegistro = Instant.now();
    }

    public Long getId() { return id; }
    public Internacao getInternacao() { return internacao; }
    public User getUsuario() { return usuario; }
    public String getAlimento() { return alimento; }
    public String getQuantidade() { return quantidade; }
    public String getAceitacaoObservacao() { return aceitacaoObservacao; }
    public Instant getDataHoraRegistro() { return dataHoraRegistro; }
}
