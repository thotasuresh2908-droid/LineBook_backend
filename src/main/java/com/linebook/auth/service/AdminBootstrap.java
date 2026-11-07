package com.linebook.auth.service;

import com.linebook.auth.domain.AppUser;
import com.linebook.auth.domain.UserRole;
import com.linebook.auth.repository.AppUserRepository;
import com.linebook.common.config.LinebookProperties;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the very first admin login on startup when the app_users table has no admin yet, using
 * {@code LINEBOOK_ADMIN_EMAIL} and {@code LINEBOOK_ADMIN_PASSWORD}.
 *
 * <p>This is how you get into a brand-new production database: set the two variables, deploy, sign
 * in, then change the password from inside the app. It never touches an existing admin, so leaving
 * the variables set on later deploys does nothing.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordHasher hasher;
    private final LinebookProperties props;

    public AdminBootstrap(AppUserRepository users, PasswordHasher hasher, LinebookProperties props) {
        this.users = users;
        this.hasher = hasher;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(UserRole.ADMIN)) {
            return;
        }
        String email = props.auth().adminEmail();
        String password = props.auth().adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No admin login exists and LINEBOOK_ADMIN_EMAIL / LINEBOOK_ADMIN_PASSWORD are not set; "
                    + "nobody can sign in yet.");
            return;
        }
        users.save(new AppUser(
                String.format("us-%04d", users.count() + 1),
                email.trim().toLowerCase(Locale.ROOT),
                props.auth().adminName(),
                hasher.hash(password.trim()),
                UserRole.ADMIN,
                null,
                true));
        log.info("Created the first admin login for {}. Change this password after signing in.", email);
    }
}
