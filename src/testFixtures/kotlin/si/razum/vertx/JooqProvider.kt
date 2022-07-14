package si.razum.vertx

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockExecuteContext
import org.jooq.tools.jdbc.MockResult

/**
 * A container for a ready-to-use jOOQ connection that can be used in tests.
 * Deprecated?
 */
/*interface JooqProvider: AutoCloseable {
    val jooq: DSLContext

    companion object {

        /**
         * Provides an auto-closeable source for jOOQ connections
         * @property pgVersion Version of Postgres to use, e.g. "13"
         */
        fun provide(pgVersion: String): JooqProvider {
            return PostgresContainer(pgVersion)
        }

        /** Useful when we just want a simple mock */
        fun provideMockContext(): DSLContext {
            val connection = MockConnection(object: MockDataProvider {
                override fun execute(ctx: MockExecuteContext?): Array<MockResult> {
                    return emptyArray()
                }
            })
            return DSL.using(connection, SQLDialect.POSTGRES)
        }
    }
}*/
