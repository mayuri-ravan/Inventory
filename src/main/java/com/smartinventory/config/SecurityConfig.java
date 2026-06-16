package com.smartinventory.config;

import com.smartinventory.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Main Spring Security configuration.
 * - Stateless JWT-based authentication
 * - Role-based access control per endpoint
 * - CORS configuration
 * - BCrypt password encoding
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.max-age}")
    private long corsMaxAge;

    /** Public endpoints that don't require authentication */
    private static final String[] PUBLIC_URLS = {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless API)
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Session management - stateless
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                    // Admin-only endpoints
                    .requestMatchers("/admin/**").hasRole("ADMIN")

                    // Admin + Manager endpoints
                    .requestMatchers("/managers/**").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/products/*/approve").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers(HttpMethod.PUT, "/products/*/reject").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers("/warehouses/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                    .requestMatchers("/inventory/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                    .requestMatchers("/dashboard/**").authenticated()
                    .requestMatchers("/notifications/**").authenticated()
                    .requestMatchers("/settings/**").authenticated()
                    .requestMatchers("/profile/**").authenticated()
                    .requestMatchers("/categories/**").authenticated()
                    .requestMatchers("/purchase-orders/**").hasAnyRole("ADMIN", "MANAGER", "STAFF", "SUPPLIER")
                    .requestMatchers("/smart-analytics/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                    .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers("/audit-logs/**").hasRole("ADMIN")

                    // Supplier endpoints
                    .requestMatchers("/suppliers/**").hasAnyRole("ADMIN", "MANAGER", "SUPPLIER")

                    // Customer endpoints
                    .requestMatchers("/orders/**").authenticated()

                    // All other requests require authentication
                    .anyRequest().authenticated()
            )

            // Use JWT filter instead of session-based auth
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(corsMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 12 for production
    }
}
