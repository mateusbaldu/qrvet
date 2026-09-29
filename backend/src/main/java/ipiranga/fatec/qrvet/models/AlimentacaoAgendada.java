package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alimentacao_agendada")
public class AlimentacaoAgendada {
    public enum Status { PENDENTE, CONCLUIDA, CANCELADA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "internacao_id", nullable = false)
    private Internacao internacao;
    @Column(nullable = false, length = 100) private String alimento;
    @Column(nullable = false, length = 50) private String quantidade;
    @Column(nullable = false, columnDefinition = "TIMESTAMP(6)") private Instant horario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.PENDENTE;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registro_id") private RegistroAlimentacao registro;
    @Column(name = "motivo_cancelamento", length = 255) private String motivoCancelamento;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "criado_por", nullable = false) private User criadoPor;

    protected AlimentacaoAgendada() {}
    public AlimentacaoAgendada(Internacao internacao, String alimento, String quantidade, Instant horario, User criadoPor) {
        this.internacao = internacao; this.alimento = alimento; this.quantidade = quantidade;
        this.horario = horario; this.criadoPor = criadoPor;
    }
    public Long getId() { return id; }
    public Internacao getInternacao() { return internacao; }
    public String getAlimento() { return alimento; }
    public String getQuantidade() { return quantidade; }
    public Instant getHorario() { return horario; }
    public Status getStatus() { return status; }
    public RegistroAlimentacao getRegistro() { return registro; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void complete(RegistroAlimentacao registro) { this.registro = registro; status = Status.CONCLUIDA; }
    public void cancel(String reason) { motivoCancelamento = reason; status = Status.CANCELADA; }
}
