package org.jazz.jazzflix.dto;


import org.jazz.jazzflix.entity.user.Gender;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    private String email;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String dateOfBirth;
    private List<String> roles;
}