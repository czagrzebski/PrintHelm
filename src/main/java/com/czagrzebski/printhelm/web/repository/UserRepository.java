package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
