package com.czagrzebski.printhelm.web.specification;

import com.czagrzebski.printhelm.web.model.Privilege;
import com.czagrzebski.printhelm.web.model.Role;
import com.czagrzebski.printhelm.web.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


public class UserSpecification implements Specification<User> {

    @Override
    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.conjunction();
    }

    public static Specification<User> hasUsername(String username) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (username == null || username.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("username"), username);
        };
    }

    public static Specification<User> hasEmail(String email) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (email == null || email.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("email"), email);
        };
    }

    public static Specification<User> hasRole(String role) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (role == null || role.isEmpty()) {
                return cb.conjunction();
            }
            Join<User, Role> roleJoin = root.join("userRoles", JoinType.INNER);
            return cb.equal(roleJoin.get("roleName"), role);
        };
    }

    public static Specification<User> hasPrivilege(String privilege) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (privilege == null || privilege.isEmpty()) {
                return cb.conjunction();
            }
            Join<User, Role> roleJoin = root.join("userRoles", JoinType.INNER);
            Join<Role, Privilege> privilegeJoin = roleJoin.join("privileges", JoinType.INNER);
            return cb.equal(privilegeJoin.get("privilegeName"), privilege);
        };
    }

    public static Specification<User> buildSpecification(String username, String email, String role, String privilege) {
        return Specification.where(hasUsername(username))
                .and(hasEmail(email))
                .and(hasRole(role))
                .and(hasPrivilege(privilege));
    }
}
