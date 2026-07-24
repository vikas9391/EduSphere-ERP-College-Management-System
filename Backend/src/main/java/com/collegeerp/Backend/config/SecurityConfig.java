package com.collegeerp.Backend.config;

import com.collegeerp.Backend.common.dto.ApiResponse;
import com.collegeerp.Backend.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * {@code @EnableMethodSecurity} turns on {@code @PreAuthorize} support - without it,
 * annotations added to controllers (see e.g. {@code DepartmentController},
 * {@code AttendanceController}) are silently ignored and every endpoint stays
 * reachable by any authenticated user regardless of role, which was the situation
 * this whole application was in previously (see README_PROGRESS.md).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**"
    );

    /**
     * College registration and tenant management are platform-operator actions, not
     * something any authenticated tenant user should be able to do — restricted to the
     * SUPER_ADMIN role, whose only account is seeded via {@code SuperAdminSeeder} and
     * who logs in through {@code POST /api/auth/super-admin/login} (schema="public",
     * no collegeCode). This used to be in PUBLIC_ENDPOINTS above with no auth at all.
     */
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /**
     * Comma-separated list, e.g. "https://myapp.vercel.app,https://admin.myapp.com".
     * Defaults to the local Vite dev server so nothing changes for local development —
     * set CORS_ALLOWED_ORIGINS in the deployed backend's environment to add the real
     * frontend domain(s), including the super admin login origin if it's served
     * separately.
     */
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(PUBLIC_ENDPOINTS.toArray(new String[0])).permitAll()
                    .requestMatchers("/api/tenants/**").hasRole(SUPER_ADMIN_ROLE)
                    .anyRequest().authenticated()
            )
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Ensures unauthenticated requests to protected endpoints get the same
     * ApiResponse JSON shape as every other error in the app, instead of
     * Spring Security's default empty 401/403 body.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeJsonError(response, 401, request.getRequestURI(), "Authentication required");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJsonError(response, 403, request.getRequestURI(), "Access denied");
    }

    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status, String path, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(message, status, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
