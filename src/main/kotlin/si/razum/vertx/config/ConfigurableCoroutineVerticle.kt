package si.razum.vertx.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ConfigurableCoroutineVerticle(val log: Logger): CoroutineVerticle() {

    private lateinit var configurationChangeListener: MessageConsumer<JsonObject>

    override suspend fun start() {
        if (!config.isEmpty) {
            // Config coming in via deployment options
            readConfiguration(config, true) // will fail hard on a bad config, this is intentional!
        } else {
            // Config coming in via configSource
            val storedConfig = vertx.eventBus().request<JsonObject>(CONFIG_INITIAL_VALUE_ADDRESS, "").await()
            readConfiguration(storedConfig.body(), true)
        }

        configurationChangeListener = vertx.eventBus().consumer(CONFIG_CHANGE_HANDLER_ADDRESS) { newConfig ->
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
        const val CONFIG_CHANGE_HANDLER_ADDRESS = "si.razum.vertx.config.configChange"

        /** Address on which we will respond to requests for initial configuration (if there is one!) */
        const val CONFIG_INITIAL_VALUE_ADDRESS = "si.razum.vertx.config.currentConfig"

        private val LOG = LoggerFactory.getLogger("si.razum.vertx.config.ConfigurableCoroutineVerticle")

        /**
         * Creates a reloadable configuration watcher that signals config changes on the
         * Vertx event bus.
         * @param classpathDefaultsPath The location of the default config in the project's classpath
         * @param overrideFileLocation A file that can override the defaults and is monitored for changes
         * @param baseConfig If provided, this config will serve as the basis before any file-based configs are applied over it
         */
        fun reloadableHoconConfig(vertx: Vertx, classpathDefaultsPath: String? = null, overrideFileLocation: String? = null, baseConfig: Any? = null): ConfigRetriever {
            // HOCON configuration format - defaults embedded in app
            val configRetrieverOptions = ConfigRetrieverOptions()

            if (baseConfig != null) {
                LOG.info("Using an underlying config object as the basis for other config")
                configRetrieverOptions.addStore(
                    ConfigStoreOptions().setType("json").setConfig(JsonObject.mapFrom(baseConfig))
                )
            }

            if (classpathDefaultsPath != null) {
                LOG.info("Reading default config from classpath at $classpathDefaultsPath")
                configRetrieverOptions.addStore(
                    ConfigStoreOptions().setType("file").setFormat("hocon")
                        .setConfig(JsonObject().put("path", classpathDefaultsPath))
                )
            }

            if (overrideFileLocation != null) {
                LOG.info("Overlaying default config with options from $overrideFileLocation")
                configRetrieverOptions.addStore(ConfigStoreOptions().apply {
                    type = "file"
                    isOptional = false
                    format = "hocon"
                    config = JsonObject().put("path", overrideFileLocation)
                })
            }

            val configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions)
            configRetriever.listen { change ->
                vertx.eventBus().publish(CONFIG_CHANGE_HANDLER_ADDRESS, change.newConfiguration)
            }
            vertx.eventBus().consumer<Unit>(CONFIG_INITIAL_VALUE_ADDRESS) { msg ->
                configRetriever.config
                .onSuccess { msg.reply(it) }
                .onFailure { msg.fail(500, it.message) }
            }

            return configRetriever
        }



    }
}
