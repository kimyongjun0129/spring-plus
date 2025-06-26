package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth (로그인, 회원가입)
                        .requestMatchers(HttpMethod.POST, "/auth/*").permitAll()

                        // Comment (댓글 생성 및 조회)
                        .requestMatchers(HttpMethod.POST, "/todos/*/comments").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos/*/comments").permitAll()

                        // Manager (매니저 생성, 조회 및 삭제)
                        .requestMatchers(HttpMethod.POST, "/todos/*/managers").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos/*/managers").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/todos/*/managers/*").hasRole(UserRole.USER.name())

                        // To do (To do 생성 및 조회)
                        .requestMatchers(HttpMethod.POST, "/todos").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/todos/*").permitAll()

                        // User (User 조회 및 수정)
                        .requestMatchers(HttpMethod.GET, "/users/*").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users").hasRole(UserRole.USER.name())

                        // Admin (User 역할 변경)
                        .requestMatchers(HttpMethod.PATCH, "/admin/users/*").hasRole(UserRole.ADMIN.name())
                        .anyRequest().hasRole(UserRole.ADMIN.name())
                )
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
