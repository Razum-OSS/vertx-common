package si.razum.vertx.eventbus

import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract class helping us to register & use a message bus messages that await a reply
 * @param I The input parameter type
 * @param O The handler output parameter type
 */
abstract class MessageBusCall<I, O: Any> (private val address: String) {
    protected val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * An exception that happened while processing a bus message - can be used to regulate the return code and message.
     */
    class HandlerException(val code: Int, message: String): RuntimeException(message)

    /** Register a callback that will be fired when this event is received */
    @Suppress("DuplicatedCode") // suspend & non-suspend blocks aren't easily interchangeable
    fun registerHandler(bus: EventBus, handler: (I) -> O): MessageConsumer<I> =
        bus.localConsumer(address) { message ->
            val args = message.body()
            val (response, success) = try {
                handler(args) to true
            } catch(ex: HandlerException) {
                message.fail(ex.code, ex.message)
                null to false
            } catch (t: Throwable) {
                log.error("Unhandled error in message handler", t)
                message.fail(500, t.message)
                null to false
            }
            // not in the above try/catch because we don't want to mask _sending_ exceptions
            if (success) message.localReply(response as Any)
        }

    /** Register a callback that will be fired when this event is received */
    @Suppress("DuplicatedCode") // suspend & non-suspend blocks aren't easily interchangeable
    fun registerSuspendHandler(verticle: CoroutineVerticle, handler: suspend (I) -> O): MessageConsumer<I> =
        verticle.vertx.eventBus().localConsumer(address) { message ->
            val args = message.body()
            verticle.launch {
                val (response, success) = try {
                    handler(args) to true
                } catch(ex: HandlerException) {
                    message.fail(ex.code, ex.message)
                    null to false
                } catch (t: Throwable) {
                    log.error("Unhandled error in message handler", t)
                    message.fail(500, t.message)
                    null to false
                }
                // not in the above try/catch because we don't want to mask _sending_ exceptions
                if (success) message.localReply(response!!)
            }
        }


    /** Send this message and receive a future on which the response can be awaited */
    fun request(bus: EventBus, args: I): Future<Message<O>> =
        bus.localRequest(address, args as Any)
}
