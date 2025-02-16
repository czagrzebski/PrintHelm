package com.czagrzebski.printhelm.web.listener;

import com.czagrzebski.printhelm.web.Application;
import com.czagrzebski.printhelm.web.model.Privilege;
import com.czagrzebski.printhelm.web.model.Role;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.repository.PrivilegeRepository;
import com.czagrzebski.printhelm.web.repository.RoleRepository;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LogManager.getLogger(SetupDataLoader.class);
    boolean setupComplete = false;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupDataLoader(UserRepository userRepository, RoleRepository roleRepository,
                           PrivilegeRepository privilegeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(!setupComplete) {
            logger.info("Loading default admin data into the database");
            Privilege readPrivilege
                    = createPrivilegeIfNotFound("READ_PRIVILEGE", "Read Privilege");
            Privilege writePrivilege
                    = createPrivilegeIfNotFound("WRITE_PRIVILEGE", "Write Privilege");
            Set<Privilege> adminPrivileges = Set.of(readPrivilege, writePrivilege);
            Role adminRole =  createRoleIfNotFound("ROLE_ADMIN", "Admin Privileges", adminPrivileges);

            if(userRepository.findByUsername("admin") == null) {
                User user = new User("admin", passwordEncoder.encode("changeme123"), "admin", "admin");
                user.setUserRoles(Set.of(adminRole));
                user.setActive(true);
                userRepository.save(user);
            }

            logger.info("Admin data load complete!");
            setupComplete = true;
        }
    }

    @Transactional
    Privilege createPrivilegeIfNotFound(String privilegeName, String privilegeDescription) {
        Privilege privilege = privilegeRepository.findByPrivilegeName(privilegeName);
        if (privilege == null) {
            privilege = new Privilege(privilegeName, privilegeDescription);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    @Transactional
    Role createRoleIfNotFound(
            String roleName, String roleDescription, Set<Privilege> privileges) {

        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            role = new Role(roleName, roleDescription, privileges);
            roleRepository.save(role);
        }
        return role;
    }
}
