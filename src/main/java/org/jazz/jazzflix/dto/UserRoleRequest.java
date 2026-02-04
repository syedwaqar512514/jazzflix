package org.jazz.jazzflix.dto;

import org.jazz.jazzflix.entity.user.UserRole;
import lombok.Data;

@Data
public class UserRoleRequest {
    private UserRole role;
}
