package ipiranga.fatec.qrvet.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserAccount {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @Column(length = 40)
    private String crmv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "avatar_data", columnDefinition = "LONGBLOB")
    private byte[] avatarData;

    @Column(name = "avatar_content_type", length = 80)
    private String avatarContentType;

    protected UserAccount() {}

    public UserAccount(Clinic clinic, String name, String email, String passwordHash, Role role) {
        this.clinic = clinic;
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public Clinic getClinic() { return clinic; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public String getCrmv() { return crmv; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getCreatedAt() { return createdAt; }
    public byte[] getAvatarData() { return avatarData; }
    public String getAvatarContentType() { return avatarContentType; }

    public void markActivity() { this.lastActivityAt = Instant.now(); }
    public void changeRole(Role role) { this.role = role; }
    public void deactivate() { this.status = UserStatus.INACTIVE; }
    public void updateProfile(String name, String email, String phone, String crmv) {
        this.name = name.trim();
        this.email = normalizeEmail(email);
        this.phone = blankToNull(phone);
        this.crmv = blankToNull(crmv);
    }
    public void changePassword(String passwordHash) { this.passwordHash = passwordHash; }
    public void updateAvatar(byte[] data, String contentType) {
        this.avatarData = data;
        this.avatarContentType = contentType;
    }
    public void removeAvatar() { updateAvatar(null, null); }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
