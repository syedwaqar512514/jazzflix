package org.jazz.jazzflix.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class WatchModel {
    private UUID uuid;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private String slug;
    private String type;
    private String thumbnail;
    private String genera;
    private String rating;
    private ContentModel watchContent;
}

@Getter
@AllArgsConstructor
class ContentModel {
    private UUID uuid;
    private String videoUrl1080p;
    private String videoUrl720p;
    private String videoUrl480p;
    private String videoUrl380p;
}

