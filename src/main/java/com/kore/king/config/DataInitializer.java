package com.kore.king.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kore.king.entity.User;
import com.kore.king.entity.UserRole;
import com.kore.king.repository.UserRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    
    @Value("${app.admin.default-username:admin}")
    private String adminUsername;
    
    @Value("${app.admin.default-password:}")
    private String adminPassword;
    
    @Value("${app.admin.default-email:admin@betking.com}")
    private String adminEmail;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        try {
            // Your initialization code here
            // Check if we're in dev profile
            boolean isDevProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev"));
        
            // Validate admin password in production
            if ((adminPassword == null || adminPassword.trim().isEmpty()) && !isDevProfile) {
                throw new IllegalStateException("Admin password must be set in production environment. Set app.admin.default-password in application.properties");
            }

            // Create default admin user if not exists
            if (userRepository.findByUsername(adminUsername).isEmpty()) {
                String passwordToUse = adminPassword.isEmpty() ? "admin123" : adminPassword;
                
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(passwordToUse));
                admin.setRole(UserRole.MAIN_ADMIN);
                admin.setAvailablePoints(10000);
                
                userRepository.save(admin);
                System.out.println("Default admin user created: " + adminUsername);
                
                if (adminPassword.isEmpty()) {
                    System.out.println("⚠️ WARNING: Using default admin password. Change immediately in production!");
                }
            }
            
            // Create sample employee admin for testing in dev profile
            if (userRepository.findByUsername("employee").isEmpty() && isDevProfile) {
                User employee = new User();
                employee.setUsername("employee");
                employee.setEmail("employee@betking.com");
                employee.setPassword(passwordEncoder.encode("employee123"));
                employee.setRole(UserRole.EMPLOYEE_ADMIN);
                employee.setAvailablePoints(5000);
                
                userRepository.save(employee);
                System.out.println("Sample employee admin created: employee / employee123");
            }
        } catch (Exception e) {
            System.err.println("Data initialization failed: " + e.getMessage());
            System.err.println("Application will continue to start...");
            // Don't rethrow the exception to allow app to start
        }
    }
}