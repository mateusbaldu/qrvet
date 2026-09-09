package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jejum")
public class Jejum {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Internacao internacao;
        private String motivo;

        public Builder internacao(Internacao internacao) {
            this.internacao = internacao;
            return this;
        }

        public Builder motivo(String motivo) {
            this.motivo = motivo;
            return this;
        }

        public Jejum build() {
            Jejum jejum = new Jejum();
            jejum.internacao = internacao;
            jejum.motivo = motivo;
            return jejum;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false)
    private Internacao internacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "data_hora_inicio", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataHoraInicio;

    @Column(name = "data_hora_fim", columnDefinition = "TIMESTAMP(6)")
    private Instant dataHoraFim;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    protected Jejum() {}

    @PrePersist
    void initializeStart() {
        dataHoraInicio = Instant.now();
    }

    public void end() {
        if (!ativo) {
            throw new IllegalStateException("The fasting period has already been closed.");
        }
        ativo = false;
        dataHoraFim = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Internacao getInternacao() {
        return internacao;
    }

    public Instant getDataHoraInicio() {
        return dataHoraInicio;
    }

    public Instant getDataHoraFim() {
        return dataHoraFim;
    }

    public String getMotivo() {
        return motivo;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
