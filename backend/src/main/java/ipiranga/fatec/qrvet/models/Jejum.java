package ipiranga.fatec.qrvet.models;

import jakarta.persistence.*;

@Entity
@Table(name = "jejum")
public class Jejum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internacao_id", nullable = false)
    private Internacao internacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    protected Jejum() {}

    public boolean isAtivo() {
        return ativo;
    }
}
