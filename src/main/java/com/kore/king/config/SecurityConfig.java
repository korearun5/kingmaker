package com.kore.king.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.kore.king.config.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    //private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsService userDetailsService
                         ) {
        this.userDetailsService = userDetailsService;
        //this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Increased strength
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Configuration
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/ws/**", "/api/**", "/bets/**", "/app/**") // Only exclude necessary endpoints
            )
            
            // Session Management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            
            // Authorization
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/register", "/login", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                //.requestMatchers("/ws/**").permitAll()
                
                // User endpoints
                .requestMatchers("/dashboard", "/user/**", "/buy-points/**", "/withdraw/**").hasAnyRole("USER", "EMPLOYEE_ADMIN", "MAIN_ADMIN")
                
                // Admin endpoints with role hierarchy
                .requestMatchers("/admin/payments/**", "/admin/settings/**", "/admin/create-admin").hasRole("MAIN_ADMIN")
                .requestMatchers("/admin/users/**", "/admin/transactions/**", "/admin/support/**", "/admin/game-ids/**").hasAnyRole("MAIN_ADMIN", "EMPLOYEE_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("MAIN_ADMIN", "EMPLOYEE_ADMIN")
                // In your SecurityConfig
                .requestMatchers("/ws/**", "/api/**", "/app/**", "/topic/**", "/queue/**", "/user/**").permitAll()

                // Secure all other endpoints
                .anyRequest().authenticated()
            )
            
            // Form Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            
            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            
            // Headers Security
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self' ws: wss:;")
                )
                .frameOptions().deny()
                .xssProtection().disable() // Let CSP handle XSS
            )
            
            // Exception Handling
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/error/403")
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            );

        return http.build();
    }

    // Add security event publisher for auditing
    @Bean
    public ApplicationListener<AbstractAuthenticationEvent> authenticationEventListener() {
        return event -> {
            if (event instanceof AuthenticationSuccessEvent) {
                // Log successful authentication
                System.out.println("User authenticated: " + event.getAuthentication().getName());
            } else if (event instanceof AuthenticationFailureBadCredentialsEvent) {
                // Log failed authentication attempts
                System.out.println("Failed authentication attempt: " + event.getAuthentication().getName());
            }
        };
    }
}