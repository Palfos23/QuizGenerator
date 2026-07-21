package com.quizapp.config;

import com.quizapp.model.AdminUser;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.AdminUserRepository;
import com.quizapp.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

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

        questionRepository.saveAll(List.of(
                // English
                question("What is the capital of France?", "Geography", 2, Language.EN, "Paris", false),
                question("Which planet is known as the Red Planet?", "Science", 2, Language.EN, "Mars", false),
                question("Who directed the movie 'Jaws'?", "Movies", 5, Language.EN, "Steven Spielberg", false),
                question("What is the chemical symbol for gold?", "Science", 4, Language.EN, "Au", false),
                question("In what year did the Berlin Wall fall?", "History", 7, Language.EN, "1989", false),
                question("Which country has won the most FIFA World Cups?", "Sports", 6, Language.EN, "Brazil", true),
                question("Who is the Premier League's all-time top scorer?", "Sports", 6, Language.EN, "Alan Shearer", true),
                question("What is the capital of Norway?", "Geography", 1, Language.EN, "Oslo", false),

                // German
                question("Was ist die Hauptstadt von Deutschland?", "Geography", 1, Language.DE, "Berlin", false),
                question("Wer schrieb 'Faust'?", "Literature", 5, Language.DE, "Johann Wolfgang von Goethe", false),
                question("Welches Element hat das Symbol 'Fe'?", "Science", 4, Language.DE, "Eisen", false),

                // French
                question("Quelle est la capitale de l'Espagne ?", "Geography", 2, Language.FR, "Madrid", false),
                question("Qui a peint la Joconde ?", "Movies", 3, Language.FR, "Léonard de Vinci", false),
                question("En quelle année a eu lieu la Révolution française ?", "History", 6, Language.FR, "1789", false),

                // Spanish
                question("¿Cuál es la capital de Italia?", "Geography", 1, Language.ES, "Roma", false),
                question("¿Quién escribió 'Don Quijote'?", "Literature", 5, Language.ES, "Miguel de Cervantes", false),
                question("¿Cuántos huesos tiene el cuerpo humano adulto?", "Science", 7, Language.ES, "206", false),

                // Norwegian
                question("Hva er hovedstaden i Sverige?", "Geography", 1, Language.NO, "Stockholm", false),
                question("Hvem skrev 'Et dukkehjem'?", "Literature", 6, Language.NO, "Henrik Ibsen", false),
                question("Hvilket år ble Norge selvstendig fra Sverige?", "History", 7, Language.NO, "1905", false)
        ));
    }

    private Question question(String text, String category, int difficultyLevel, Language language,
                               String answer, boolean couldChange) {
        Question q = new Question();
        q.setQuestionText(text);
        q.setCategory(category);
        q.setDifficultyLevel(difficultyLevel);
        q.setLanguage(language);
        q.setAnswer(answer);
        q.setCouldChange(couldChange);
        return q;
    }
}
