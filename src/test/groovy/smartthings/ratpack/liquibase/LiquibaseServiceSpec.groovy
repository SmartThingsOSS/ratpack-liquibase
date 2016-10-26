package smartthings.ratpack.liquibase

import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseConnection
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.mop.ConfineMetaClassChanges

import javax.sql.DataSource

@ConfineMetaClassChanges([Liquibase, JdbcConnection])
class LiquibaseServiceSpec extends Specification {

    def "Migration not performed if autoMigrate is false"() {
        given:
        LiquibaseModule.Config config = new LiquibaseModule.Config(autoMigrate: false)
        DataSource dataSource = Mock()
        LiquibaseService service = Spy(LiquibaseService, constructorArgs: [config, dataSource])

        when:
        service.onStart(null);

        then:
        0 * service.migrate()
        1 * service.onStart(null)
        0 * _
    }

    @Unroll
    def "Migration performed if autoMigrate is true - autoCommit = #autoCommit"() {
        given:
        LiquibaseModule.Config config = new LiquibaseModule.Config(autoMigrate: true)
        DataSource dataSource = Mock()
        DatabaseConnection connection = Mock()
        Liquibase liquibase = Mock()
        Database database = Mock()
        LiquibaseService service = Spy(LiquibaseService, constructorArgs: [config, dataSource])

        when:
        service.onStart(null);

        then:
        1 * service.onStart(null)
        1 * service.migrate()
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        1 * liquibase.update('')
        liquibase.database >> database
        1 * database.autoCommit >> autoCommit
        commitCalls * database.commit()
        1 * connection.close()
        0 * _

        where:
        autoCommit | commitCalls
        true       | 0
        false      | 1
    }

    def "Transaction rolled back if migration error occurs"() {
        given:
        LiquibaseModule.Config config = new LiquibaseModule.Config(autoMigrate: true)
        DataSource dataSource = Mock()
        DatabaseConnection connection = Mock()
        Liquibase liquibase = Mock()
        Database database = Mock()
        LiquibaseService service = Spy(LiquibaseService, constructorArgs: [config, dataSource])

        when:
        service.onStart(null);

        then:
        1 * service.onStart(null)
        1 * service.migrate()
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        1 * liquibase.update('') >> { throw new LiquibaseException("migration failed") }
        0 * liquibase.database
        1 * connection.rollback()
        1 * connection.close()
        0 * _

        where:
        autoCommit | commitCalls
        true       | 0
        false      | 1
    }
}
