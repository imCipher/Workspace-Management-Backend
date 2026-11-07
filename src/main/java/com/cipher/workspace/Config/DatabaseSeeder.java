package com.cipher.workspace.Config;

import com.cipher.workspace.Entity.*;
import com.cipher.workspace.Repository.PaymentTypeRepository;
import com.cipher.workspace.Repository.ServiceTypeRepository;
import com.cipher.workspace.Repository.UserRepository;
import com.cipher.workspace.Services.ExpenseCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpenseCategoryService expenseCategoryService;
    private final ServiceTypeRepository serviceTypeRepository;
    private final PaymentTypeRepository paymentTypeRepository;

    @Value("${app.default-admin.username:${app.admin.username:admin}}")
    private String defaultAdminUsername;

    @Value("${app.default-admin.password:${app.admin.password:Admin@12345}}")
    private String defaultAdminPassword;

    @Value("${app.default-admin.email:${app.admin.email:admin@workspace.com}}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.fullname:${app.admin.fullname:Workspace Administrator}}")
    private String defaultAdminFullname;

    public DatabaseSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ExpenseCategoryService expenseCategoryService,
            ServiceTypeRepository serviceTypeRepository,
            PaymentTypeRepository paymentTypeRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.expenseCategoryService = expenseCategoryService;
        this.serviceTypeRepository = serviceTypeRepository;
        this.paymentTypeRepository = paymentTypeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDefaultAdmin();
        seedDefaultOptions();
    }

    private void seedDefaultAdmin() {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            log.info("No system administrator found. Provisioning default administrator from application.properties...");

            User admin = userRepository.findByUsername(defaultAdminUsername)
                    .or(() -> userRepository.findByEmail(defaultAdminEmail))
                    .orElseGet(User::new);

            admin.setUsername(defaultAdminUsername);
            admin.setEmail(defaultAdminEmail);
            admin.setFullname(admin.getFullname() != null && !admin.getFullname().isBlank() ? admin.getFullname() : defaultAdminFullname);
            if (admin.getPassword() == null || admin.getPassword().isBlank()) {
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            }
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);

            userRepository.save(admin);
            log.info("Default administrator provisioned successfully: username='{}', email='{}', status={}, emailVerified={}",
                    admin.getUsername(), admin.getEmail(), admin.getStatus(), admin.isEmailVerified());
        } else {
            log.debug("System administrator already exists. Skipping default admin provisioning.");
        }
    }

    private void seedDefaultOptions() {
        // Seed expense categories
        try {
            expenseCategoryService.ensureDefaultCategories();
        } catch (Exception e) {
            log.warn("Could not seed default expense categories: {}", e.getMessage());
        }

        // Seed service types
        try {
            if (serviceTypeRepository.count() == 0) {
                List<ServiceTypeEntity> defaults = new ArrayList<>();
                for (ServiceType st : ServiceType.values()) {
                    defaults.add(new ServiceTypeEntity(st.name(), st.getDisplayName(), true));
                }
                serviceTypeRepository.saveAll(defaults);
                log.info("Seeded default service types: {}", defaults.size());
            }
        } catch (Exception e) {
            log.warn("Could not seed default service types: {}", e.getMessage());
        }

        // Seed payment types
        try {
            if (paymentTypeRepository.count() == 0) {
                List<PaymentTypeEntity> defaults = new ArrayList<>();
                for (PaymentType pt : PaymentType.values()) {
                    defaults.add(new PaymentTypeEntity(pt.name(), pt.getDisplayName(), true));
                }
                paymentTypeRepository.saveAll(defaults);
                log.info("Seeded default payment types: {}", defaults.size());
            }
        } catch (Exception e) {
            log.warn("Could not seed default payment types: {}", e.getMessage());
        }
    }
}
