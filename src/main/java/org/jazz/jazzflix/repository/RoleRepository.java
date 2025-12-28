package org.jazz.jazzflix.repository;

import org.jazz.jazzflix.entity.user.Role;
import org.jazz.jazzflix.entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByUserRole(UserRole userRole);
}