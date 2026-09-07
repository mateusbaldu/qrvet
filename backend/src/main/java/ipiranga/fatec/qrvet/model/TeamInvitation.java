package ipiranga.fatec.qrvet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_invitations")
public class TeamInvitation {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false, length = 190)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TeamInvitation() {}

    public TeamInvitation(Clinic clinic, String email, Role role, String tokenHash, Instant expiresAt) {
        this.clinic = clinic;
        this.email = UserAccount.normalizeEmail(email);
        this.role = role;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = InvitationStatus.PENDING;
    }

    @PrePersist
    void prePersist() {
        if (invitedAt == null) invitedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Clinic getClinic() { return clinic; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public InvitationStatus getStatus() { return status; }
    public Instant getInvitedAt() { return invitedAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public void renew(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.invitedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = InvitationStatus.PENDING;
    }
    public void cancel() { this.status = InvitationStatus.CANCELLED; }
    public void accept() { this.status = InvitationStatus.ACCEPTED; }
    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
    public void markExpired() { this.status = InvitationStatus.EXPIRED; }
}
