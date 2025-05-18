package com.czagrzebski.printhelm.web.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="User")
public class User {
    @Id
    @Column(name="user_id")
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long userId;

    @ManyToMany
    @JsonManagedReference
    @JoinTable(
            name="UserRole",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id")}
    )
    Set<Role> userRoles = new HashSet<>();

    @Column(name="username", length=50, nullable=false, unique=true)
    private String username;

    @Column(name="password_hash", length=255, nullable=false, unique=false)
    private String passwordHash;

    @Column(name="firstname", length=50, nullable=false, unique=false)
    private String firstname;

    @Column(name="lastname", length=50, nullable=false, unique=false)
    private String lastname;

    @Column(name="is_active")
    private boolean isActive;

    public User() {}

    public User(String username, String passwordHash, String firstname, String lastname) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<Role> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<Role> userRoles) {
        this.userRoles = userRoles;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
