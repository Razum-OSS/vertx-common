package si.razum.vertx.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthChecks
import io.vertx.ext.healthchecks.Status
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import si.razum.vertx.db.jooq.VertxThreadExecuteListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val LOG = LoggerFactory.getLogger(ConnectionPool::class.java)

/** Represents a singleton from which the data source can be obtained */
object ConnectionPool {

    /**
     * Holds all initiated connections.
     * The main use for multiple connections are unit tests, prod could currently quite happily live with just 1.
     */
    private val connections = ConcurrentHashMap<DbConfig, DataSource>()

    /** Returns the underlying data source */
    fun dataSource(conf: DbConfig): DataSource {
        // If multiple data sources were ever used in production we'd need to check if the DS was already closed, as well!
        return connections.getOrPut(conf) {
            LOG.info("Opening new datasource")
            setupConnectionPool(conf)
        }
    }


    /** Wraps the given datasource with jOOQ and optionally protects it from running on a Vertx event thread */
    fun wrapWithJooq(conf: DbConfig, protectEventThread: Boolean): DSLContext {
        val cfg = DefaultConfiguration().apply {
            setDataSource(dataSource(conf))
            setSQLDialect(SQLDialect.POSTGRES)
            if (protectEventThread) {
                LOG.warn("Extra Vert.X thread protection is ON - SQL queries may throw exceptions simply for being run from wrong thread!")
                setExecuteListener(VertxThreadExecuteListener())
            }
        }
        return DSL.using(cfg)
    }

    /**
     * Creates a Hikari connection pool connecting to a postgres database.
     */
    fun setupConnectionPool(cfg: DbConfig): DataSource {
        val LOG = LoggerFactory.getLogger("si.razum.vertx.db.setupConnectionPool")

        LOG.info("Setting up DB connection pool")
        val props = Properties().apply {
            setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
            setProperty("dataSource.databaseName", cfg.database)
            setProperty("dataSource.user", cfg.user)
            setProperty("dataSource.password", cfg.password)

            if (cfg.socketFile == null) {
                LOG.info("Database connection via TCP/IP to ${cfg.host}")
                LOG.info("Database port: ${cfg.port}")

                setProperty("dataSource.serverName", cfg.host)
                setProperty("dataSource.portNumber", cfg.port.toString())
            } else {
                LOG.info("Database connection via socket file ${cfg.socketFile}")

                setProperty("dataSource.serverName", "localhost")
                setProperty("dataSource.socketFactory", "org.newsclub.net.unix.AFUNIXSocketFactory\$FactoryArg")
                setProperty("dataSource.socketFactoryArg", cfg.socketFile)
                setProperty("dataSource.sslMode", "disable")
            }
            LOG.info("Database name: ${cfg.database}")
        }

        return HikariDataSource(HikariConfig(props))
    }

    /** Migrates the database using Flyway. Includes development data in migrations if [includeDevMigrations] is true. */
    fun migrateDatabase(cfg: DbConfig, includeDevMigrations: Boolean, allowOutOfOrder: Boolean) {
        val LOG = LoggerFactory.getLogger("si.razum.vertx.db.migrateDatabase")
        configureFlyway(LOG, dataSource(cfg), includeDevMigrations, allowOutOfOrder).migrate()
    }

    fun verifyDatabaseMigrations(cfg: DbConfig, includeDevMigrations: Boolean, allowOutOfOrder: Boolean) {
        val LOG = LoggerFactory.getLogger("si.razum.vertx.db.migrateDatabase")
        LOG.info("Verifying migrations")
        configureFlyway(LOG, dataSource(cfg), includeDevMigrations, allowOutOfOrder).validate()
    }

    /** Closes a data source & removes it from the internal map */
    fun closeDataSource(cfg: DbConfig) {
        connections.remove(cfg)?.let { ds ->
            (ds as HikariDataSource).close()
        }
    }

    private fun configureFlyway(LOG: Logger, ds: DataSource, includeDevMigrations: Boolean, allowOutOfOrder: Boolean) = Flyway.configure().apply {
        ignoreMissingMigrations(true)
        ignorePendingMigrations(true)
        mixed(true)
        outOfOrder(allowOutOfOrder)
        if (includeDevMigrations) {
            LOG.warn("Development migrations are being included!")
            locations("db/migration" ,"db/devMigration")
        }
        dataSource(ds)
    }.load()

    /** Registers a health check */
    fun registerDBHealthcheck(hc: HealthChecks, jooq: DSLContext) {
        hc.register("db", 1000) { promise ->
            jooq.selectOne().fetchAsync().handle { _, thrown ->
                if (thrown != null) {
                    promise.complete(
                        Status.KO(JsonObject().put("error", thrown.javaClass.name).put("message", thrown.message)
                    ))
                } else {
                    promise.complete(Status.OK())
                }
            }
        }
    }

}
