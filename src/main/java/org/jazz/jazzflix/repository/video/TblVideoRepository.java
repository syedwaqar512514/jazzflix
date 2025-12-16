package org.jazz.jazzflix.repository.video;

import org.jazz.jazzflix.entity.video.TblVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TblVideoRepository extends JpaRepository<TblVideo, UUID> {
}