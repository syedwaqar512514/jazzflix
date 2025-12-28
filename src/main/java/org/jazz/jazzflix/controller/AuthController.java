package org.jazz.jazzflix.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jazz.jazzflix.dto.AuthResponse;
import org.jazz.jazzflix.dto.LoginRequest;
import org.jazz.jazzflix.dto.Request;
import org.jazz.jazzflix.dto.Response;
import org.jazz.jazzflix.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Response<AuthResponse> login(@RequestBody Request<LoginRequest> request, HttpServletRequest httpRequest){
        String token = authService.login(request.getData().getEmail(), request.getData().getPassword());

        return new Response<AuthResponse>(
                true,
                "Login Successfully",
                new AuthResponse(token),
                HttpStatus.OK.value(),
                httpRequest.getRequestURI(),
                LocalDateTime.now().toString()
        );
    }
}
