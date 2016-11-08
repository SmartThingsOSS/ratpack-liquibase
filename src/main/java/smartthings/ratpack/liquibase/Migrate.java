package smartthings.ratpack.liquibase;

import com.google.common.io.Resources;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ratpack.config.ConfigData;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Execute Liquibase migrations outside of Ratpack startup.
 */
public class Migrate {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: smartthings.ratpack.liquibase.Migrate <username> <password> <config>");
            System.exit(1);
        }
        try {
            Path path = Paths.get(args[2]);
            ConfigData configData = Files.exists(path) ? fromPath(path) : fromClasspath(args[2]);
            HikariConfig hikariConfig = configData.get("/db", HikariConfig.class);
            hikariConfig.setUsername(args[0]);
            hikariConfig.setPassword(args[1]);
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            try {
                LiquibaseModule.Config liquibaseConfig = configData.get("/liquibase", LiquibaseModule.Config.class);
                new LiquibaseService(liquibaseConfig, dataSource).migrate();
            } finally {
                dataSource.close();
            }
        } catch (Exception ex) {
            System.err.println("Migration error:");
            ex.printStackTrace(System.err);
            System.exit(42);
        }
    }

    private static ConfigData fromPath(Path path) throws Exception {
        return ConfigData.of(c -> c.yaml(path));
    }

    private static ConfigData fromClasspath(String resourceName) throws Exception {
        URL resource = Resources.getResource(resourceName);
        return ConfigData.of(c -> c.yaml(resource));
    }
}
