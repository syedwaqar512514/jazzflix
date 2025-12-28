package org.jazz.jazzflix.service;

import org.jazz.jazzflix.dto.UserRegisterRequest;
import org.jazz.jazzflix.entity.user.*;
import org.jazz.jazzflix.repository.RoleRepository;
import org.jazz.jazzflix.repository.UserProfileRepository;
import org.jazz.jazzflix.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(UserRegisterRequest userData) {
        if (userRepository.existsByEmail(userData.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

//        User user = new User();
//        user.setEmail(userData.getEmail());
//        user.setPassword(passwordEncoder.encode(userData.getPassword()));
//        user.setEnabled(true);
//        user.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
//        user.setRoles(userData.getRoles()
//                .stream()
//                .map(role -> Role.builder().userRole(UserRole.valueOf(role)).user(user).build())
//                .collect(Collectors.toSet()));
//        user.setUserProfile(UserProfile.builder()
//                .firstName(userData.getFirstName())
//                .lastName(userData.getLastName())
//                .gender(userData.getGender())
//                .dateOfBirth(LocalDate.parse(userData.getDateOfBirth()))
//                .user(user)
//                .build());

        UserProfile userProfile = UserProfile.builder()
                .firstName(userData.getFirstName())
                .lastName(userData.getLastName())
                .gender(userData.getGender())
                .dateOfBirth(LocalDate.parse(userData.getDateOfBirth()))
                .build();

        Set<Role> roles = userData.getRoles()
                .stream()
                .map(role -> Role.builder().userRole(UserRole.valueOf(role)).build())
                .collect(Collectors.toSet());

        User user = new User();
        user.setEmail(userData.getEmail());
        user.setPassword(passwordEncoder.encode(userData.getPassword()));
        user.setEnabled(true);
        user.setRoles(roles);
        user.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        user.setUserProfile(userProfile);

        userProfile.setUser(user);
        roles.forEach(role -> role.setUser(user));


        userRepository.save(user);
        // Assign default role VIEWER
//        Role viewerRole = roleRepository.findByUserRole(UserRole.VIEWER)
//                .orElseThrow(() -> new RuntimeException("Default role not found"));
//        user.setRoles(Set.of(viewerRole));


//        // Create user profile
//        UserProfile profile = new UserProfile();
//        profile.setUser(user);
//        profile.setFirstName(firstName);
//        profile.setLastName(lastName);
//        profile.setGender(gender);
//        profile.setDateOfBirth(dob);
//
//        profileRepository.save(profile);

        return user;
    }

    public Optional<User> getUser(UUID userId) {
        return userRepository.findById(userId);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(UUID userId, String email, String firstName, String lastName, Gender gender, LocalDate dob) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        user.setEmail(email);
        userRepository.save(user);

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setGender(gender);
        profile.setDateOfBirth(dob);
        profileRepository.save(profile);

        return user;
    }

    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        profileRepository.deleteById(userId);
        userRepository.deleteById(userId);
    }

    public User assignRole(UUID userId, UserRole roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByUserRole(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<Role> roles = user.getRoles();
        roles.add(role);
        user.setRoles(roles);

        return userRepository.save(user);
    }
}