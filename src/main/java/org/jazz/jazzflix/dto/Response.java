package org.jazz.jazzflix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Response <T>{
    private boolean success;
    private String message;
    private T data;
    private int status;
    private String path;
    private String timestamp;
}
