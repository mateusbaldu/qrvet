package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "evolucao")
public class Evolucao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false, updatable = false)
    private Internacao internacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private User usuario;

    @Column(name = "profissional_nome", nullable = false, length = 120, updatable = false)
    private String profissionalNome;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String texto;

    @Column(name = "data_hora_registro", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant dataHoraRegistro;

    protected Evolucao() {}

    public Evolucao(Internacao internacao, User usuario, String texto) {
        this.internacao = internacao;
        this.usuario = usuario;
        this.profissionalNome = usuario.getName();
        this.texto = texto;
    }

    @PrePersist
    void initializeTimestamp() { dataHoraRegistro = Instant.now(); }

    public Long getId() { return id; }
    public Internacao getInternacao() { return internacao; }
    public User getUsuario() { return usuario; }
    public String getProfissionalNome() { return profissionalNome; }
    public String getTexto() { return texto; }
    public Instant getDataHoraRegistro() { return dataHoraRegistro; }
}
