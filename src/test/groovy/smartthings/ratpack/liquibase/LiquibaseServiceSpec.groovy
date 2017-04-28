package smartthings.ratpack.liquibase

import com.zaxxer.hikari.HikariConfig
import groovy.sql.Sql
import liquibase.Liquibase
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.database.Database
import liquibase.database.DatabaseConnection
import liquibase.exception.LiquibaseException
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.hikari.HikariModule
import spock.lang.Specification
import spock.lang.Unroll

import javax.sql.DataSource

class LiquibaseServiceSpec extends Specification {

    def "Migration not performed if autoMigrate is false"() {
        given:
        LiquibaseModule.Config config = new LiquibaseModule.Config(autoMigrate: false)
        DataSource dataSource = Mock()
        DatabaseConnection connection = Mock()
        Liquibase liquibase = Mock()
        LiquibaseService service = Spy(LiquibaseService, constructorArgs: [config, dataSource])

        when:
        service.onStart(null)

        then:
        1 * service.onStart(null)
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        0 * service.migrate(liquibase)
        1 * liquibase.listUnrunChangeSets(_, _) >> []
        1 * connection.close()
        0 * _
    }

    def "Service fails to start if unrun migrations exist"() {
        given:
        LiquibaseModule.Config config = new LiquibaseModule.Config(autoMigrate: false)
        DataSource dataSource = Mock()
        DatabaseConnection connection = Mock()
        Liquibase liquibase = Mock()
        LiquibaseService service = Spy(LiquibaseService, constructorArgs: [config, dataSource])

        when:
        service.onStart(null)

        then:
        thrown(LiquibaseException)

        and:
        1 * service.onStart(null)
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        0 * service.migrate(liquibase)
        1 * liquibase.listUnrunChangeSets(_, _) >> [new ChangeSet(new DatabaseChangeLog())]
        1 * connection.close()
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
        service.onStart(null)

        then:
        1 * service.onStart(null)
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        1 * liquibase.update('')
        liquibase.database >> database
        1 * database.autoCommit >> autoCommit
        commitCalls * database.commit()
        1 * liquibase.listUnrunChangeSets(_, _) >> []
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
        service.onStart(null)

        then:
        thrown(LiquibaseException)

        and:
        1 * service.onStart(null)
        1 * service.constructConnection(dataSource) >> connection
        1 * service.constructLiquibase(config, connection) >> liquibase
        1 * liquibase.update('') >> { throw new LiquibaseException("migration failed") }
        1 * liquibase.database >> database
        1 * database.rollback()
        1 * connection.close()
        0 * _

        where:
        autoCommit | commitCalls
        true       | 0
        false      | 1
    }

    def "Auto migration on service startup"() {
        expect:
        wipeDatabase()
        GroovyEmbeddedApp.ratpack {
            bindings {
                moduleConfig(LiquibaseModule.class, new LiquibaseModule.Config(autoMigrate: true, migrationFile: "migrations.xml"))
                moduleConfig(HikariModule, new HikariConfig([
                        driverClassName: 'org.h2.jdbcx.JdbcDataSource',
                        username       : 'sa',
                        password       : '',
                        jdbcUrl        : 'jdbc:h2:mem:test;INIT=CREATE SCHEMA IF NOT EXISTS test'
                ]))
            }
            handlers {
                get { DataSource dataSource ->
                    Sql sql = new Sql(dataSource)
                    render(messageColumnExists(sql) ? 'Pass' : 'Fail')
                }
            }
        }.test {
            assert getText() == 'Pass'
        }
    }

    def "Manual CLI migration"() {
        given:
        wipeDatabase()
        Sql sql = Sql.newInstance("jdbc:h2:mem:test", "sa", "", "org.h2.Driver")

        when:
        Migrate.main('sa', '', 'test.yml')

        then:
        assert messageColumnExists(sql)
    }

    private static void wipeDatabase() {
        Sql.newInstance("jdbc:h2:mem:test", "sa", "", "org.h2.Driver").execute("DROP ALL OBJECTS DELETE FILES")
    }

    private static boolean messageColumnExists(Sql sql) {
        return sql.firstRow("SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ratpack-liquibase' AND COLUMN_NAME = 'MESSAGE'").getAt(0) == 1
    }
}
