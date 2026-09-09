package ipiranga.fatec.qrvet.models;

import ipiranga.fatec.qrvet.models.enums.BaiaStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "baia")
public class Baia {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String identificacao;
        private BaiaStatus status = BaiaStatus.DISPONIVEL;
        private String observacao;

        public Builder identificacao(String identificacao) {
            this.identificacao = identificacao;
            return this;
        }

        public Builder status(BaiaStatus status) {
            this.status = status;
            return this;
        }

        public Builder observacao(String observacao) {
            this.observacao = observacao;
            return this;
        }

        public Baia build() {
            Baia baia = new Baia();
            baia.identificacao = identificacao;
            baia.status = status;
            baia.observacao = observacao;
            return baia;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificacao", nullable = false, length = 50, unique = true)
    private String identificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BaiaStatus status;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected Baia() {}

    public Long getId() {
        return id;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public BaiaStatus getStatus() {
        return status;
    }

    public void updateDetails(String identificacao, String observacao) {
        this.identificacao = identificacao;
        this.observacao = observacao;
    }

    public void changeStatusFromAdministration(BaiaStatus requestedStatus) {
        if (requestedStatus == BaiaStatus.OCUPADA) {
            throw new IllegalStateException("A bay cannot be manually set to occupied.");
        }
        if (status == BaiaStatus.OCUPADA && requestedStatus == BaiaStatus.DISPONIVEL) {
            throw new IllegalStateException("An occupied bay cannot be manually released.");
        }
        if (requestedStatus == BaiaStatus.MANUTENCAO && status != BaiaStatus.DISPONIVEL) {
            throw new IllegalStateException("Only an available bay can be placed under maintenance.");
        }
        status = requestedStatus;
    }

    public void occupyAutomatically() {
        if (status != BaiaStatus.DISPONIVEL) {
            throw new IllegalStateException("Only an available bay can be occupied.");
        }
        status = BaiaStatus.OCUPADA;
    }

    public void releaseAutomatically() {
        if (status != BaiaStatus.OCUPADA) {
            throw new IllegalStateException("Only an occupied bay can be released.");
        }
        status = BaiaStatus.DISPONIVEL;
    }
}
