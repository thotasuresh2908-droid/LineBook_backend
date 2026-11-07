package com.linebook.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A login account. The email is the login id; the password is stored only as a PBKDF2 hash. */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "email", length = 190, nullable = false, updatable = false)
    private String email;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 16, nullable = false)
    private UserRole role;

    /** The agents row this login belongs to; null for ADMIN. */
    @Column(name = "agent_id", length = 64, updatable = false)
    private String agentId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    protected AppUser() {
        // JPA
    }

    public AppUser(String id, String email, String fullName, String passwordHash,
                   UserRole role, String agentId, boolean active) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.agentId = agentId;
        this.active = active;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeFullName(String fullName) {
        this.fullName = fullName;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void markLogin(LocalDateTime at) {
        this.lastLoginAt = at;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public String getAgentId() { return agentId; }
    public boolean isActive() { return active; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
