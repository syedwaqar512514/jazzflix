package org.jazz.jazzflix.repository.video;

import org.jazz.jazzflix.entity.video.TblVideoAssest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TblVideoAssestRepository extends JpaRepository<TblVideoAssest, UUID> {
}
