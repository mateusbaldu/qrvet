package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registro_medicacao")
public class RegistroMedicacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false, updatable = false)
    private Internacao internacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private User usuario;

    @Column(name = "medicamento", nullable = false, length = 100, updatable = false)
    private String medicamento;

    @Column(name = "dose", nullable = false, length = 50, updatable = false)
    private String dose;

    @Column(name = "unidade", nullable = false, length = 30, updatable = false)
    private String unidade;

    @Column(name = "via", nullable = false, length = 50, updatable = false)
    private String via;

    @Column(name = "prescricao", nullable = false, length = 255, updatable = false)
    private String prescricao;

    @Column(name = "observacoes", columnDefinition = "TEXT", updatable = false)
    private String observacoes;

    @Column(name = "data_hora_registro", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataHoraRegistro;

    protected RegistroMedicacao() {}

    @PrePersist
    void initializeTimestamp() {
        dataHoraRegistro = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Internacao getInternacao() {
        return internacao;
    }

    public void setInternacao(Internacao internacao) {
        this.internacao = internacao;
    }

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(String medicamento) {
        this.medicamento = medicamento;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getPrescricao() {
        return prescricao;
    }

    public void setPrescricao(String prescricao) {
        this.prescricao = prescricao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Instant getDataHoraRegistro() {
        return dataHoraRegistro;
    }

    public void setDataHoraRegistro(Instant dataHoraRegistro) {
        this.dataHoraRegistro = dataHoraRegistro;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Internacao internacao;
        private User usuario;
        private String medicamento;
        private String dose;
        private String unidade;
        private String via;
        private String prescricao;
        private String observacoes;

        public Builder internacao(Internacao internacao) {
            this.internacao = internacao;
            return this;
        }

        public Builder usuario(User usuario) {
            this.usuario = usuario;
            return this;
        }

        public Builder medicamento(String medicamento) {
            this.medicamento = medicamento;
            return this;
        }

        public Builder dose(String dose) {
            this.dose = dose;
            return this;
        }

        public Builder unidade(String unidade) {
            this.unidade = unidade;
            return this;
        }

        public Builder via(String via) {
            this.via = via;
            return this;
        }

        public Builder prescricao(String prescricao) {
            this.prescricao = prescricao;
            return this;
        }

        public Builder observacoes(String observacoes) {
            this.observacoes = observacoes;
            return this;
        }

        public RegistroMedicacao build() {
            RegistroMedicacao registro = new RegistroMedicacao();
            registro.internacao = internacao;
            registro.usuario = usuario;
            registro.medicamento = medicamento;
            registro.dose = dose;
            registro.unidade = unidade;
            registro.via = via;
            registro.prescricao = prescricao;
            registro.observacoes = observacoes;
            return registro;
        }
    }
}
