
package com.hackathon.config;

import com.hackathon.security.CustomUserDetailsService;
import com.hackathon.security.JwtAuthFilter;

import com.hackathon.service.AuthService;
import com.hackathon.service.auth.CustomOAuth2UserService;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    @Lazy // 2.  hoãn khởi tạo AuthService
    private AuthService authService;
    @Autowired
    @Lazy
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DefaultOAuth2AuthorizationRequestResolver requestResolver) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. TÍCH HỢP ĐĂNG NHẬP GOOGLE OAUTH2
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                // Spring lưu các param custom vào Session ngầm
                                .authorizationRequestResolver(requestResolver)
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        //các API công khai ai cũng vào được
                        .requestMatchers(
                                "/api/account/**",
                                "/api/account/login",
                                "/api/account/resend-verification",
                                "/api/notifications/**",
                                "/error",
                                "/verify-email",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/events/public/**",
                                "/api/teams/active",
                                "/api/github/**",
                                "/api/ranking/rounds/*/topN",
                                "/api/analytics/**",
                                "/api/teams/active"
                        ).permitAll()

                        // all
                        .requestMatchers(HttpMethod.GET,"/api/ranking/rounds/*/all").hasAnyRole("EVENTCOORDINATOR","EXPERT", "STUDENT","ADMIN")
                        //EXPERT
                        .requestMatchers(HttpMethod.GET, "/api/participants/teams/**").hasRole("EXPERT")

                        // API DÀNH CHO EVENT , EXPERT, COORDINATOR
                        .requestMatchers(HttpMethod.GET, "/api/teams/expert/**")
                        .hasAnyRole("EXPERT", "ADMIN", "EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/teams/*/detail")
                        .hasAnyRole("ADMIN", "EXPERT", "EVENTCOORDINATOR")
                        // Xem thành viên team
                        .requestMatchers(HttpMethod.GET, "/api/teams/members/**")
                        .hasAnyRole("STUDENT", "EXPERT", "ADMIN", "EVENTCOORDINATOR")

                        // Dành cho STUDENT (Đăng ký sự kiện & Mời thành viên)
                        .requestMatchers(HttpMethod.POST, "/api/registrations/*/register-event").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/registrations/teams/invite").hasRole("STUDENT")
                        //student nộp bài
                        .requestMatchers(HttpMethod.POST, "/api/submissions/*/create").hasRole("STUDENT")
                        .requestMatchers("/api/teams/**").hasRole("STUDENT")

                        // TeamRequest - STUDENT
                        .requestMatchers(HttpMethod.POST, "/api/team-request/direct").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/team-request").hasRole("STUDENT")

                        // TeamRequest - EXPERT
                        .requestMatchers(HttpMethod.GET, "/api/team-request/received").hasRole("EXPERT")
                        .requestMatchers(HttpMethod.GET, "/api/events/all").hasAnyRole("EXPERT", "EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/team-request/*/reject").hasRole("EXPERT")
                        .requestMatchers(HttpMethod.PATCH, "/api/team-request/*/accept").hasRole("EXPERT")
                        .requestMatchers(HttpMethod.GET, "/api/team-request/appeal/*/review-submissions").hasRole("EXPERT")
                        .requestMatchers("/api/expert/assigments/**").hasRole("EXPERT")

                        //Chỉ event coordinator
                        // 1. tất cả các API thay đổi dữ liệu sự kiện (POST, PUT, DELETE, PATCH)
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/events/update-time-event/*"
                        ).hasRole("EVENTCOORDINATOR")
                        .requestMatchers("/api/events/create", "/api/events/publish/**",
                                "/api/events/delete/**", "/api/events/update/**",
                                "/api/events/restore/**", "/api/events/cancel/**", "/api/events/permanently/**", "/api/events/check-minimum-teams")
                        .hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.POST, "/api/round/advancement/*").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/export/**", "/api/dashboard/**")
                        .hasAnyRole("EVENTCOORDINATOR", "ADMIN")

                        // 2. các API xem danh sách sự kiện (GET)
                        .requestMatchers("/api/events/trash", "/api/events/experts", "/api/events/all", "/api/events/search-all")
                        .hasRole("EVENTCOORDINATOR")
                        .requestMatchers("/api/criteriaSet/**")
                        .hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/round/*/participant/detail").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/participants/teams/disqualify/**").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*/draw-results/**").hasRole("EVENTCOORDINATOR")

                        // Dành cho COORDINATOR (Quản lý duyệt đơn)
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*/approveTeam-detail").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*/approved-teams").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/registrations/*/approve").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/registrations/*/reject").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*/pendingTeam").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*/pendingTeam-detail").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/registrations/*/count-registration").hasRole("EVENTCOORDINATOR")

                        // TeamRequest - EVENT COORDINATOR
                        .requestMatchers(HttpMethod.PATCH, "/api/team-request/*/process").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/team-request/event/*").hasRole("EVENTCOORDINATOR")
                        .requestMatchers(HttpMethod.GET, "/api/team-request/appeal/*").hasRole("EVENTCOORDINATOR")

                        //Dành cho ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/roles/permissions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/roles/permissions").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").authenticated()
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

    @Bean
    public DefaultOAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        // BSpring Security: "Hãy lưu cái param tên là 'action' vào OAuth2 Request
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> {
                    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attrs != null) {
                        String action = attrs.getRequest().getParameter("action");
                        if (action != null) {
                            params.put("action", action);
                        }
                    }
                })
        );
        return resolver;
    }
}
