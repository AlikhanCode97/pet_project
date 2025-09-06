package com.example.Games.user;

import com.example.Games.user.balance.Balance;
import jakarta.persistence.*;
import lombok.*;
import com.example.Games.user.role.Role;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Balance balance;
    
    // Account status fields
    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;
    
    @Builder.Default
    @Column(name = "is_account_non_locked", nullable = false)
    private Boolean isAccountNonLocked = true;
    
    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;
    
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;
    
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isAccountNonExpired() {
        return true; // Can be enhanced with expiration logic
    }
    
    public boolean isAccountNonLocked() {
        if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
            return false;
        }
        return isAccountNonLocked;
    }
    
    public boolean isCredentialsNonExpired() {
        return true; // Can be enhanced with password expiration logic
    }
    
    public boolean isEnabled() {
        return isEnabled && isEmailVerified; // Account is enabled AND email is verified
    }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
    
    public void lockAccount(int lockDurationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }
    
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
