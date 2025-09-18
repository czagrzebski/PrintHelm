package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.Privilege;
import com.czagrzebski.printhelm.web.domain.Role;
import com.czagrzebski.printhelm.web.domain.User;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import jakarta.transaction.Transactional;
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
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       User user = userRepository.findByUsername(username);
       if(user == null) {
           throw new UsernameNotFoundException("Unable to find user");
       }
       var authorities = getAuthorities(user.getUserRoles());
       List<GrantedAuthority> grantedAuthorities =
               authorities.stream().map(SimpleGrantedAuthority::new)
                       .collect(Collectors.toUnmodifiableList());

       return new org.springframework.security.core.userdetails.User(
               user.getUsername(),
               user.getPasswordHash(),
               grantedAuthorities
       );
    }

    public List<String> getAuthorities(Collection<Role> roles) {
        List<String> authorities = new ArrayList<>();
        for(Role role : roles) {
            var rolePrivileges = role.getPrivileges();
            for(Privilege privilege : rolePrivileges) {
                authorities.add(privilege.getPrivilegeName());
            }
            authorities.add(role.getRoleName());
        }
        return authorities;
    }
}
