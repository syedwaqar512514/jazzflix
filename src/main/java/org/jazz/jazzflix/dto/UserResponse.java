package org.jazz.jazzflix.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.jazz.jazzflix.entity.user.UserRole;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private UserRole role;
    private boolean enabled;
}
