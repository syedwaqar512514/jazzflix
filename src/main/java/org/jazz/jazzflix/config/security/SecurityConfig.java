package org.jazz.jazzflix.config.security;

import org.jazz.jazzflix.config.security.jwt.JwtAuthenticationFilter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoint
                        .requestMatchers(
//                                "/jazz/js/common-auth.js",
                                "/common-auth.js",
//                                "/upload-progress.html",
                                "/auth.html",
                                "/login.html",
//                                "/dashboard-user.html",
//                                "/dashboard-admin.html",
//                                "/manage-user.html",
//                                "/update-user.html",
                                "/jazz/register.html",
                                "/auth/login",
                                "/auth/debug",
                                "/api/users/register",
//                                "/video/api/dash/**",
//                                "/video/api/thumbnail/**",
//                                "/video/api/upload",
                                "/jazz/auth/login"
//                                "/jazz/video/api/getAll",
//                                "/jazz/video/api/upload"
                        ).permitAll()

                        // Upload & Management
//                        .requestMatchers("/jazz/video/api/upload").hasRole("CREATOR")

                        //Only Admins
//                        .requestMatchers("/users/**", "/dashboard-admin.html").hasRole("ADMIN")

                        //Everything else
                        .anyRequest().permitAll()
                )
                // Add jwt filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    ApplicationRunner debugSecurityFilters(ApplicationContext context) {
        return args -> {
            FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
            proxy.getFilterChains().forEach(chain -> {
                System.out.println("🔗 Security chain:");
                chain.getFilters().forEach(f ->
                        System.out.println("  ➜ " + f.getClass().getSimpleName()));
            });
        };
    }
}
