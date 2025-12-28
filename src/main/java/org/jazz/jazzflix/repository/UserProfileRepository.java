package org.jazz.jazzflix.repository;


import org.jazz.jazzflix.entity.user.User;
import org.jazz.jazzflix.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
