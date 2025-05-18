package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.model.Privilege;
import com.czagrzebski.printhelm.web.model.Role;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       User user = userRepository.findByUsername(username);
       if(user == null) {
           throw new UsernameNotFoundException("Unable to find user");
       }
       var privileges = getPrivileges(user.getUserRoles());
       List<GrantedAuthority> authorities =
               privileges.stream().map(SimpleGrantedAuthority::new)
                       .collect(Collectors.toUnmodifiableList());
       return new org.springframework.security.core.userdetails.User(
               user.getUsername(),
               user.getPasswordHash(),
               authorities
       );
    }

    public List<String> getPrivileges(Collection<Role> roles) {
        List<String> privileges = new ArrayList<>();
        for(Role role : roles) {
            var rolePrivileges = role.getPrivileges();
            for(Privilege privilege : rolePrivileges) {
                privileges.add(privilege.getPrivilegeName());
            }
        }
        return privileges;
    }
}
