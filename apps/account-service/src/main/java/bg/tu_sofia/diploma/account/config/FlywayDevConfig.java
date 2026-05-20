package bg.tu_sofia.diploma.account.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Local-dev only: wipe the schema and re-run all migrations on every startup.
 * Enabled by {@code app.db.clean-on-start=true} (set in the root .env, never in
 * Kubernetes), which also requires {@code spring.flyway.clean-disabled=false}.
 * This keeps the database disposable while the schema is still changing.
 */
@Configuration
@ConditionalOnProperty(name = "app.db.clean-on-start", havingValue = "true")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
