package com.quizapp.config;

import com.quizapp.model.AdminUser;
import com.quizapp.repository.AdminUserRepository;
import com.quizapp.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username:admin}")
    private String defaultAdminUsername;

    @Value("${app.admin.default-password:changeme123}")
    private String defaultAdminPassword;

    public DataSeeder(QuestionRepository questionRepository,
                       AdminUserRepository adminUserRepository,
                       PasswordEncoder passwordEncoder) {
        this.questionRepository = questionRepository;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedQuestions();
    }

    private void seedAdmin() {
        if (adminUserRepository.count() > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(defaultAdminUsername);
        admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
        adminUserRepository.save(admin);
        System.out.println("Seeded default admin user '" + defaultAdminUsername +
                "' - change app.admin.default-password before deploying!");
    }

    private void seedQuestions() {
        if (questionRepository.count() > 0) {
            return;
        }
        questionRepository.saveAll(SampleQuestions.build());
    }
}
