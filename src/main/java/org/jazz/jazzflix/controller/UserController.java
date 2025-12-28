package org.jazz.jazzflix.controller;

import org.jazz.jazzflix.config.security.CustomUserDetails;
import org.jazz.jazzflix.dto.*;
import org.jazz.jazzflix.entity.user.Gender;
import org.jazz.jazzflix.entity.user.User;
import org.jazz.jazzflix.entity.user.UserRole;
import org.jazz.jazzflix.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Response<User>> register(@RequestBody Request<UserRegisterRequest> request) {
        UserRegisterRequest data = request.getData();
        User user = userService.registerUser(data);

        Response<User> response = new Response<>(true, "User registered", user,
                HttpStatus.CREATED.value(), "/api/users/register", java.time.LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<User>> getUser(@PathVariable UUID id) {
        User user = userService.getUser(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Response<User> response = new Response<>(true, "User retrieved", user,
                HttpStatus.OK.value(), "/api/users/" + id, java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Response<User>> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUser(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Response<User> response = new Response<>(true, "User retrieved", user,
                HttpStatus.OK.value(), "/api/users/me" , java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<Response<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        Response<List<User>> response = new Response<>(true, "All users", users,
                HttpStatus.OK.value(), "/api/users", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response<User>> updateUser(@PathVariable UUID id,
                                                     @RequestBody Request<UserUpdateRequest> request) {
        UserUpdateRequest data = request.getData();
        User updatedUser = userService.updateUser(id, data.getEmail(),
                data.getFirstName(), data.getLastName(), data.getGender(),
                LocalDate.parse(data.getDateOfBirth(), formatter));

        Response<User> response = new Response<>(true, "User updated", updatedUser,
                HttpStatus.OK.value(), "/api/users/" + id, java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        Response<Void> response = new Response<>(true, "User deleted", null,
                HttpStatus.OK.value(), "/api/users/" + id, java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<Response<User>> assignRole(@PathVariable UUID id,
                                                     @RequestBody Request<UserRoleRequest> request) {
        UserRoleRequest data = request.getData();
        User updatedUser = userService.assignRole(id, data.getRole());

        Response<User> response = new Response<>(true, "Role assigned", updatedUser,
                HttpStatus.OK.value(), "/api/users/" + id + "/role", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}

