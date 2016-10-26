package smartthings.ratpack.liquibase;

import com.google.inject.Scopes;
import ratpack.guice.ConfigurableModule;

/**
 * Ratpack Liquibase Guice module.
 */
public class LiquibaseModule extends ConfigurableModule<LiquibaseModule.Config> {

    /**
     * Ratpack Liquibase configuration.
     */
    public static class Config {
        String migrationFile = "migrations.xml";
        String contexts = "";
        boolean autoMigrate = false;

        public String getMigrationFile() {
            return migrationFile;
        }

        public void setMigrationFile(String migrationFile) {
            this.migrationFile = migrationFile;
        }

        public String getContexts() {
            return contexts;
        }

        public void setContexts(String contexts) {
            this.contexts = contexts;
        }

        public boolean isAutoMigrate() {
            return autoMigrate;
        }

        public void setAutoMigrate(boolean autoMigrate) {
            this.autoMigrate = autoMigrate;
        }
    }

    @Override
    protected void configure() {
        bind(LiquibaseService.class).in(Scopes.SINGLETON);
    }
}
