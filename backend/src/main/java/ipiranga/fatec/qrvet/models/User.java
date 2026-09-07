package ipiranga.fatec.qrvet.models;

import ipiranga.fatec.qrvet.models.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;


@Entity
@Table(name = "usuario")
public class User {
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
}
