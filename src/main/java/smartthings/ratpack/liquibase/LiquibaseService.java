package smartthings.ratpack.liquibase;

import com.google.inject.Inject;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * {@link ratpack.service.Service} that performs Liquibase migrations on startup.
 */
public class LiquibaseService implements Service {

    private final LiquibaseModule.Config config;
    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(LiquibaseService.class);

    @Inject
    public LiquibaseService(LiquibaseModule.Config config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
    }

    /**
     * Server startup event.
     * Executed after the root registry and server instance are constructed and before the server begins accepting
     * requests.
     *
     * @param event meta information about the startup event
     * @throws SQLException thrown if a connection to the database cannot be established
     */
    @Override
    public void onStart(StartEvent event) throws SQLException {
        if (config.autoMigrate) {
            migrate();
        }
    }

    /**
     * Performs Liquibase migration.
     *
     * @throws SQLException thrown in a connection to the database cannot be established
     */
    public void migrate() throws SQLException {
        logger.info("Starting migrations for {}", config.migrationFile);

        DatabaseConnection connection = constructConnection(dataSource);
        try {
            Liquibase liquibase = constructLiquibase(config, connection);
            liquibase.update(config.contexts);

            if (!liquibase.getDatabase().isAutoCommit()) {
                liquibase.getDatabase().commit();
            }
        } catch (LiquibaseException any) {
            logger.warn("An error occurred executing migrations", any);
            try {
                connection.rollback();
            } catch (DatabaseException e) {
                logger.warn("Could not roll back migration transaction", e);
            }
        } finally {
            try {
                connection.close();
            } catch (DatabaseException e) {
                logger.warn("An error occurred closing connection after migrations", e);
            }
        }
    }

    DatabaseConnection constructConnection(DataSource dataSource) throws SQLException {
        return new JdbcConnection(dataSource.getConnection());
    }

    Liquibase constructLiquibase(LiquibaseModule.Config config, DatabaseConnection connection)
            throws LiquibaseException {
        return new Liquibase(config.migrationFile, new ClassLoaderResourceAccessor(), connection);
    }

    /**
     * Server stop event.
     * Executed after the root handler stops accepting requests and before the server closes the channel and thread
     * pool.
     *
     * @param event meta information about the stop event
     */
    @Override
    public void onStop(StopEvent event) {
    }
}
