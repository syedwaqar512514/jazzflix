package org.jazz.jazzflix.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jazz.jazzflix.config.security.CustomUserDetails;
import org.jazz.jazzflix.service.CustomUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws jakarta.servlet.ServletException, java.io.IOException {

        String header = request.getHeader("Authorization");

        log.info("Request URI: {}"+request.getRequestURI());
        log.info("Authorization Header: {}", header);

        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("No JWT token found");
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        log.info("✅ JWT Token detected");


        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("🔐 Token validated");
        String email = jwtService.extractUsername(token);

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("🧠 SecurityContext updated: {}",
                SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);
    }
}
