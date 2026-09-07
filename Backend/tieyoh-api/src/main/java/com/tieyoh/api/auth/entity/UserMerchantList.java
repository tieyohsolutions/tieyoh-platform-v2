package com.tieyoh.api.auth.entity;

import com.tieyoh.api.merchant.entity.Merchant;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_merchant_list",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "merchant_id"})
)
public class UserMerchantList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserMerchantList() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
