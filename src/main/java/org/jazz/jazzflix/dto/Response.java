package org.jazz.jazzflix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Response <T>{
    private boolean success;
    private String message;
    private T data;
    private int status;
    private String path;
    private String timestamp;
}
