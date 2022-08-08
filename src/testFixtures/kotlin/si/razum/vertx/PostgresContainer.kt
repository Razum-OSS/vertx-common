package si.razum.vertx

import org.apache.commons.lang3.RandomStringUtils
import org.testcontainers.containers.PostgreSQLContainer
import si.razum.vertx.db.ConnectionPool
import si.razum.vertx.db.ConnectionPool.migrateDatabase
import si.razum.vertx.db.DbConfig

/**
 * Creates a Postgres container using TestContainers, runs database migrations on it and then allows us to
 * spin off fresh DB instances as templates of the migrated initial state (providing a performance boost as migrations
 * don't have to be re-run every time we need a clean slate).
 *
 * @property pgVersion Version of Postgres to use, e.g. "13"
 * @property config The database config from which we can base our initial database param
 */
class PostgresContainer(val pgVersion: String) {

    private val CLEAN_MIGRATED_DB_NAME = "sourcedatabase"

    private val container: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:$pgVersion").apply {
            withDatabaseName(CLEAN_MIGRATED_DB_NAME)
            withUsername("testuser")
            withPassword("passwwwwooorrrrd")

            start()

            // migrate so template DB will be populated, then close connection so it can be used as template
            val db = dbConfig(this, CLEAN_MIGRATED_DB_NAME)
            migrateDatabase(db, true, false)
            ConnectionPool.closeDataSource(db)
        }
    }

    /** Generates a random database name so we can spawn new ones from the original template */
    private fun databaseRandomName() = RandomStringUtils.randomAlphabetic(10).toLowerCase()

    /** Create a DBConfig object that can be passed on to other code, with the instance-specific random [dbName] */
    private fun dbConfig(container: PostgreSQLContainer<Nothing>, dbName: String) = DbConfig(
        host = "127.0.0.1", port = container.firstMappedPort, user = container.username, database = dbName, password = container.password
    )

    private fun templatedPostgres(dbNameForCopy: String) = container.apply {
        val result = execInContainer("createdb", "-U", username, "-T", CLEAN_MIGRATED_DB_NAME, dbNameForCopy)
        check (result.exitCode == 0) { "Error creating database in Postgres container" }
    }

    /** Opens a connection to a fresh database & returns the configuration object that can be used to connect to it */
    fun openConnection(): DbConfig {
        val name = databaseRandomName()
        return dbConfig(templatedPostgres(name), name)
    }

    /*override val jooq = DSL.using(postgres.apply {
        withDatabaseName(databaseRandomName)
    }.createConnection(""))*/
}
