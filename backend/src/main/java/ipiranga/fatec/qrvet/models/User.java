package ipiranga.fatec.qrvet.models;

import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;


@Entity
@Table(name = "usuario")
public class User {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String email;
        private String passwordHash;
        private Role role;
        private boolean active;
        private long authenticationVersion;
        private boolean confirmed = true;

        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder authenticationVersion(long version) { this.authenticationVersion = version; return this; }
        public Builder confirmed(boolean confirmed) { this.confirmed = confirmed; return this; }

        public User build() {
            return new User(null, name, email, passwordHash, role, active, authenticationVersion, confirmed);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 40)
    private Role role;

    @Column(name = "ativo", nullable = false)
    private boolean active;

    @Column(name = "versao_autenticacao", nullable = false)
    private long authenticationVersion;

    @Column(name = "confirmado", nullable = false)
    private boolean confirmed = true;

    @Column(name = "ultima_atividade_em", columnDefinition = "TIMESTAMP(6)")
    private Instant lastActivityAt;

    @Column(name = "criado_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "atualizado_em", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private Instant updatedAt;

    @PrePersist
    void initializeAuditTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateAuditTimestamp() {
        updatedAt = Instant.now();
    }

    public User(Long id, String name, String email, String passwordHash, Role role, boolean active, long authenticationVersion, boolean confirmed) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.authenticationVersion = authenticationVersion;
        this.confirmed = confirmed;
    }

    public User() {}

    public void setPassword(String password, PasswordEncoder encoder) {
        this.passwordHash = encoder.encode(password);
    }

    public boolean passwordMatches(String password, PasswordEncoder encoder) {
        return encoder.matches(password, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getAuthenticationVersion() {
        return authenticationVersion;
    }

    public void setAuthenticationVersion(long authenticationVersion) {
        this.authenticationVersion = authenticationVersion;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
