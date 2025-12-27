package org.jazz.jazzflix.dto;

import org.jazz.jazzflix.entity.user.Gender;
import lombok.Data;

import java.util.List;

@Data
public class UserRegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String dateOfBirth; // ISO string e.g., "1995-06-25"
    private List<String> roles;
}
