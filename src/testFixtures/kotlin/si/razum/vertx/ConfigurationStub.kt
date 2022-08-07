package si.razum.vertx

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

/**
 * This stub works with ConfigurableCoroutineVerticles, allowing us to stub configuration in tests.
 * The first config version is read from the
 */
object ConfigurationStub {

    /** The currently set configuration */
    var currentConfig: JsonObject? = null

    /** Inits configuration with the value read from the classpath's [path] */
    fun initFromClasspath(vertx: Vertx, path: String) {
        val configRetrieverOptions = ConfigRetrieverOptions()
        configRetrieverOptions.addStore(
            ConfigStoreOptions().setType("file").setFormat("hocon")
            .setConfig(JsonObject().put("path", path))
        )
        val configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions)

    }

}