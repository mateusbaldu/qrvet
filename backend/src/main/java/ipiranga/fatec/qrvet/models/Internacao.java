package ipiranga.fatec.qrvet.models;

import ipiranga.fatec.qrvet.models.enums.InternacaoStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internacao")
public class Internacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "baia_id", nullable = false)
    private Baia baia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veterinario_id", nullable = false)
    private User veterinario;

    @Column(name = "uuid_token", nullable = false, unique = true, length = 36, updatable = false)
    private UUID uuidToken;

    @Column(name = "entrada_internacao", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant entradaInternacao;

    @Column(name = "saida_internacao", columnDefinition = "TIMESTAMP(6)")
    private Instant saidaInternacao;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "diagnostico_inicial", nullable = false, length = 255)
    private String diagnosticoInicial;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InternacaoStatus status = InternacaoStatus.ATIVA;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected Internacao() {}

    @PrePersist
    void initializeEntry() {
        entradaInternacao = Instant.now();
        if (uuidToken == null) uuidToken = UUID.randomUUID();
        if (status == null) status = InternacaoStatus.ATIVA;
    }

    public void close(InternacaoStatus closingStatus) {
        if (status != InternacaoStatus.ATIVA) {
            throw new IllegalStateException("Only an active hospitalization can be closed.");
        }
        status = closingStatus;
        saidaInternacao = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Baia getBaia() {
        return baia;
    }

    public void setBaia(Baia baia) {
        this.baia = baia;
    }

    public User getVeterinario() {
        return veterinario;
    }

    public void setVeterinario(User veterinario) {
        this.veterinario = veterinario;
    }

    public UUID getUuidToken() {
        return uuidToken;
    }

    public void setUuidToken(UUID uuidToken) {
        this.uuidToken = uuidToken;
    }

    public Instant getEntradaInternacao() {
        return entradaInternacao;
    }

    public void setEntradaInternacao(Instant entradaInternacao) {
        this.entradaInternacao = entradaInternacao;
    }

    public Instant getSaidaInternacao() {
        return saidaInternacao;
    }

    public void setSaidaInternacao(Instant saidaInternacao) {
        this.saidaInternacao = saidaInternacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDiagnosticoInicial() {
        return diagnosticoInicial;
    }

    public void setDiagnosticoInicial(String diagnosticoInicial) {
        this.diagnosticoInicial = diagnosticoInicial;
    }

    public InternacaoStatus getStatus() {
        return status;
    }

    public void setStatus(InternacaoStatus status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public long getVersao() {
        return versao;
    }

    public void setVersao(long versao) {
        this.versao = versao;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Paciente paciente;
        private Baia baia;
        private User veterinario;
        private UUID uuidToken;
        private String motivo;
        private String diagnosticoInicial;
        private String observacoes;

        public Builder paciente(Paciente paciente) { this.paciente = paciente; return this; }
        public Builder baia(Baia baia) { this.baia = baia; return this; }
        public Builder veterinario(User veterinario) { this.veterinario = veterinario; return this; }
        public Builder uuidToken(UUID uuidToken) { this.uuidToken = uuidToken; return this; }
        public Builder motivo(String motivo) { this.motivo = motivo; return this; }
        public Builder diagnosticoInicial(String diagnosticoInicial) { this.diagnosticoInicial = diagnosticoInicial; return this; }
        public Builder observacoes(String observacoes) { this.observacoes = observacoes; return this; }

        public Internacao build() {
            Internacao internacao = new Internacao();
            internacao.paciente = paciente;
            internacao.baia = baia;
            internacao.veterinario = veterinario;
            internacao.uuidToken = uuidToken;
            internacao.motivo = motivo;
            internacao.diagnosticoInicial = diagnosticoInicial;
            internacao.observacoes = observacoes;
            return internacao;
        }
    }

}
