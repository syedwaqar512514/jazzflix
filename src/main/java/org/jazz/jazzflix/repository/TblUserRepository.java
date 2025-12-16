package org.jazz.jazzflix.repository;

import org.jazz.jazzflix.entity.TblUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TblUserRepository extends JpaRepository<TblUser, Long> {
    Optional<TblUser> findByEmail(String email);
}