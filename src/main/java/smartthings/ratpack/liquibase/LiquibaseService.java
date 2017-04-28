package smartthings.ratpack.liquibase;

import com.google.inject.Inject;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
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
import java.util.List;

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
     * @throws LiquibaseException thrown for migration failures or existence of unrun migrations
     * @throws SQLException thrown if a connection to the database cannot be established
     */
    @Override
    public void onStart(StartEvent event) throws LiquibaseException, SQLException {
        DatabaseConnection connection = constructConnection(dataSource);
        try {
            Liquibase liquibase = constructLiquibase(config, connection);
            if (config.autoMigrate) {
                migrate(liquibase);
            }
            checkUnmigrated(liquibase);
        } finally {
            try {
                connection.close();
            } catch (DatabaseException e) {
                logger.warn("An error occurred closing connection after migrations", e);
            }
        }
    }

    private void checkUnmigrated(Liquibase liquibase) throws LiquibaseException {
        List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
        unrunChangeSets.stream().forEach(changeSet -> logger.error("Found unrun change set: {}", changeSet));
        if (!unrunChangeSets.isEmpty()) {
            logger.error("Startup failure due to unrun change sets.");
            throw new LiquibaseException("Service failed to start due to unrun changesets");
        }
        logger.info("Migrations are up to date.");
    }

    /**
     * Performs Liquibase migration.
     *
     * @throws LiquibaseException thrown for migration failures
     * @throws SQLException thrown if a connection to the database cannot be established
     */
    public void migrate() throws LiquibaseException, SQLException {
        logger.info("Starting migrations for {}", config.migrationFile);

        DatabaseConnection connection = constructConnection(dataSource);
        try {
            Liquibase liquibase = constructLiquibase(config, connection);
            migrate(liquibase);
        } finally {
            try {
                connection.close();
            } catch (DatabaseException e) {
                logger.warn("An error occurred closing connection after migrations", e);
            }
        }
    }

    private void migrate(Liquibase liquibase) throws LiquibaseException {
        logger.info("Starting migrations for {}", config.migrationFile);

        try {
            liquibase.update(config.contexts);

            if (!liquibase.getDatabase().isAutoCommit()) {
                liquibase.getDatabase().commit();
            }
        } catch (LiquibaseException any) {
            logger.error("An error occurred executing migrations", any);
            try {
                liquibase.getDatabase().rollback();
            } catch (DatabaseException e) {
                logger.warn("Could not roll back migration transaction", e);
            } finally {
                throw any;
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
