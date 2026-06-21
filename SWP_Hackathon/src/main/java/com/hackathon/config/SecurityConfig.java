package com.hackathon.config;

import com.hackathon.security.CustomUserDetailsService;
import com.hackathon.security.JwtAuthFilter;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //các API công khai ai cũng vào được
                        .requestMatchers(
                                "/api/account/**",
                                "/api/notifications/**",
                                "/api/events/all",
                                "/api/events/detail/**",
                                "/api/events/search-all",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                                .requestMatchers("/api/criteriaSet/**").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.GET,"/api/participants/teams/**").hasRole("EXPERT")
                                .requestMatchers(HttpMethod.PUT, "/api/participants/teams/disqualify").hasAnyRole("ADMIN", "EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.PATCH, "/api/events/*/approve", "/api/events/*/reject").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.GET, "/api/events/*/approved-teams").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.PUT, "/api/events/*/draw-results").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("EVENTCOORDINATOR")
                                .requestMatchers(HttpMethod.PATCH, "/api/events/**").hasRole("EVENTCOORDINATOR")

                                // 3. API dành cho Admin
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                // 4. API dành cho Student
                                .requestMatchers("/api/teams/**").hasRole("STUDENT")

                                .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
