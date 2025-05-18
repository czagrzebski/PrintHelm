package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.model.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    Privilege findByPrivilegeName(String privilegeName);
}
