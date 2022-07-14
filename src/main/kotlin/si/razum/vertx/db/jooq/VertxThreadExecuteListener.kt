package si.razum.vertx.db.jooq

import io.vertx.core.Context
import org.jooq.ExecuteContext
import org.jooq.ExecuteListener

/** Checks whether database executions are being run on event threads */
class VertxThreadExecuteListener: ExecuteListener {
    override fun executeStart(ctx: ExecuteContext?) {
        check(!Context.isOnEventLoopThread()) { "Running database queries on the event thread is forbidden!" }
    }
}
