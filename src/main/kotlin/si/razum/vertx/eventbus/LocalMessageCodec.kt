package si.razum.vertx.eventbus

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageCodec
import io.vertx.kotlin.core.eventbus.deliveryOptionsOf


const val LOCAL_CODEC_NAME = "vertx.localonly.codec"
private val GENERIC_LOCAL_OPTIONS = deliveryOptionsOf(codecName = LOCAL_CODEC_NAME, localOnly = true)

/**
 * A message codec for the Vert.x message bus that only works locally.
 *
 * Messages passed via this message codec only work within the same JVM with the tradeoff that no wire protocol
 * (JSON or Buffer) needs to be defined for each and every message.
 *
 * To use the codec, it must first be registered using `registerLocalMessageCodec`. Messages must be sent using the
 * `localSend` or `localRequest` extension methods.
 *
 * The mechanism is described in this [article](https://dev.to/sip3/how-to-extend-vert-x-eventbus-api-to-save-on-serialization-3akf).
 *
 * @see EventBus.localSend
 * @see Vertx.registerLocalMessageCodec
 */
class LocalMessageCodec: MessageCodec<Any, Any> {
    override fun name() = LOCAL_CODEC_NAME
    override fun transform(s: Any?) = s
    override fun encodeToWire(buffer: Buffer?, s: Any?) = TODO("LocalMessageCodec cannot be used between different JVM instances - write a proper codec to encode/decode this message on the wire!")
    override fun decodeFromWire(pos: Int, buffer: Buffer?) = TODO("LocalMessageCodec cannot be used between different JVM instances - write a proper codec to encode/decode this message on the wire!")
    override fun systemCodecID(): Byte = -1
}

/**
 * Registers a new instance of LocalMessageCodec with Vert.x. It unregisters the codec if one was previously registered,
 * so this code is safe to call multiple times.
 * @return The Vert.x instance for call chaining
 */
fun Vertx.registerLocalMessageCodec(): Vertx {
    eventBus().unregisterCodec(LOCAL_CODEC_NAME).registerCodec(LocalMessageCodec())
    return this
}

/**
 * Sends a local message using the LocalMessageCodec. This makes it possible to send arbitrary messages without providing
 * a wire serializer/deserializer.
 */
fun EventBus.localSend(address: String, message: Any, options: DeliveryOptions? = null) =
    send(address, message, options.ensureLocalMessageCode)

/**
 * Sends a local message using the LocalMessageCodec waiting for a reply. This makes it possible to send arbitrary
 * messages without providing a wire serializer/deserializer. Be carefuly to reply using localReply!
 */
fun <T> EventBus.localRequest(address: String, message: Any, options: DeliveryOptions? = null): Future<Message<T>> =
    request(address, message, options.ensureLocalMessageCode)

/**
 * Replies to a message locally. This makes it possible to reply to arbitrary
 * messages without providing a wire serializer/deserializer.
 */
fun Message<*>.localReply(message: Any, options: DeliveryOptions? = null) =
    reply(message, options.ensureLocalMessageCode)

private val DeliveryOptions?.ensureLocalMessageCode get () =
    if (this == null) GENERIC_LOCAL_OPTIONS
    else apply {
        codecName = LOCAL_CODEC_NAME
        isLocalOnly = true
    }
