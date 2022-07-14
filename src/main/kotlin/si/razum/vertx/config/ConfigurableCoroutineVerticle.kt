package si.razum.vertx.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ConfigurableCoroutineVerticle(val log: Logger): CoroutineVerticle() {
    override suspend fun start() {
        log.info("Reading verticle config")
        readConfiguration(config, true) // will fail hard on a bad config, this is intentional!
        vertx.eventBus().consumer<JsonObject>(CONFIG_CHANGE_HANDLER_ADDRESS) { newConfig ->
            log.info("Configuration change - reloading config")
            try {
                launch {
                    try {
                        readConfiguration(newConfig.body(),false)
                    } catch (t: Throwable) {
                        // ignore error if we were already running, keep previous config
                        log.error("Error updating configuration", t)
                    }
                }
            } catch (t: Throwable) {
                log.error("Error applying reloaded configuration! Careful: the app may have been left between two valid configuration states!", t)
            }
        }
    }

    /**
     * The closure that handles configuration (re)reads. Code here can read things into class properties.
     * @param conf The configuration JSON (containing the entire app configuration, verticle should pick what it needs)
     * @param forceStatusOutput If true this is a hint to the logger it should log this configuration change even if
     * nothing had changed, e.g at verticle start.
     */
    abstract suspend fun readConfiguration(conf: JsonObject, forceStatusOutput: Boolean = false)

    companion object {
        /** Address on the event bus via which config changes will be propagated */
        val CONFIG_CHANGE_HANDLER_ADDRESS = "si.razum.vertx.config.configChange"

        private val LOG = LoggerFactory.getLogger("si.razum.vertx.config.ConfigurableCoroutineVerticle")

        /**
         * Creates a reloadable configuration watcher that signals config changes on the
         * Vertx event bus.
         * @param defaultLocation The location of the default config in the project's classpath
         * @param overrideLocation The location of the file with overloads for the default that is monitored for changes
         */
        fun reloadableHoconConfig(defaultLocation: String, overrideLocation: String?, vertx: Vertx): ConfigRetriever {
            // HOCON configuration format - defaults embedded in app
            val configRetrieverOptions = ConfigRetrieverOptions()
            configRetrieverOptions.addStore(
                ConfigStoreOptions().setType("file").setFormat("hocon").setConfig(JsonObject().put("path", defaultLocation))
            )

            if (overrideLocation != null) {
                LOG.info("Overlaying default config with options from $overrideLocation")
                configRetrieverOptions.addStore(ConfigStoreOptions().apply {
                    type = "file"
                    isOptional = false
                    format = "hocon"
                    config = JsonObject().put("path", overrideLocation)
                })
            }

            val configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions)
            configRetriever.listen { change ->
                vertx.eventBus().publish(CONFIG_CHANGE_HANDLER_ADDRESS, change.newConfiguration)
            }
            return configRetriever
        }
    }
}
