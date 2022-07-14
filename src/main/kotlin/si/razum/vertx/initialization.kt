package si.razum.vertx

import com.fasterxml.jackson.databind.SerializationFeature
import io.vertx.core.json.jackson.DatabindCodec
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * JSON deserialization to Kotlin classes & dates/times
 */
fun configureDataBinding() {
    DatabindCodec.mapper().registerModule(KotlinModule.Builder().build())
    DatabindCodec.mapper().registerModule(JavaTimeModule())
    DatabindCodec.mapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
