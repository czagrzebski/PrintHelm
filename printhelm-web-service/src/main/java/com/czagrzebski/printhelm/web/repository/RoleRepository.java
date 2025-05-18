package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRoleName(String roleName);
}
