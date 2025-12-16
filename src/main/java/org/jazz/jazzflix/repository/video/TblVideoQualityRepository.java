package org.jazz.jazzflix.repository.video;

import org.jazz.jazzflix.entity.video.TblVideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TblVideoQualityRepository extends JpaRepository<TblVideoQuality, UUID> {

    List<TblVideoQuality> findByVideoId(UUID videoId);

    List<TblVideoQuality> findByVideoIdAndStatus(UUID videoId, String status);

    List<TblVideoQuality> findByStatus(String status);
}