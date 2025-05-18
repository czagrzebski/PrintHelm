package com.czagrzebski.printhelm.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name="Role")
public class Role {
    @Id
    @Column(name="role_id")
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long roleId;

    @JsonBackReference
    @ManyToMany(mappedBy = "userRoles")
    private Set<User> users = new HashSet<>();

    @Column(name="role_name", length=50, nullable=false, unique=true)
    private String roleName;

    @Column(name="role_description", length=50, nullable=false, unique=false)
    private String roleDescription;

    @ManyToMany
    @JsonManagedReference
    @JoinTable(
            name = "role_privilege",
            joinColumns = { @JoinColumn(name = "role_id") },
            inverseJoinColumns = { @JoinColumn(name = "privilege_id") }
    )
    Set<Privilege> privileges = new HashSet<>();

    public Role() {}

    public Role(String roleName, String roleDescription, Set<Privilege> privileges) {
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.privileges = privileges;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Set<Privilege> privileges) {
        this.privileges = privileges;
    }
}
