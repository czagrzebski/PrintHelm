package com.czagrzebski.printhelm.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name="Privilege")
public class Privilege {
    @Id
    @Column(name="privilege_id")
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long privilegeId;

    @Column(name="privilege_name", length=50, nullable=false, unique=true)
    private String privilegeName;

    @Column(name="privilege_description", length=50, nullable=false, unique=false)
    private String privilegeDescription;

    @JsonBackReference
    @ManyToMany(mappedBy = "privileges")
    private Set<Role> roles = new HashSet<>();

    public Privilege() {}

    public Privilege(String privilegeName, String privilegeDescription) {
        this.privilegeName = privilegeName;
        this.privilegeDescription = privilegeDescription;
    }

    public Long getPrivilegeId() {
        return privilegeId;
    }

    public void setPrivilegeId(Long privilegeId) {
        this.privilegeId = privilegeId;
    }

    public String getPrivilegeName() {
        return privilegeName;
    }

    public void setPrivilegeName(String privilegeName) {
        this.privilegeName = privilegeName;
    }

    public String getPrivilegeDescription() {
        return privilegeDescription;
    }

    public void setPrivilegeDescription(String privilegeDescription) {
        this.privilegeDescription = privilegeDescription;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
