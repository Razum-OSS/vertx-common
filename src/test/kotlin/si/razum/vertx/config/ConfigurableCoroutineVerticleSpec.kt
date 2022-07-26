package si.razum.vertx.config

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FreeSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("/dev/null")

class ConfigurableCoroutineVerticleSpec : FreeSpec({

    class MyVerticle: ConfigurableCoroutineVerticle(log) {
        lateinit var foo: String

        override suspend fun readConfiguration(conf: JsonObject, forceStatusOutput: Boolean) {
            foo = conf.getString("foo")
        }
    }

    lateinit var vertx: Vertx
    beforeAny { vertx = Vertx.vertx() }

    "Fails on deploy if a bad default configuration is passed to it" {
        ConfigurableCoroutineVerticle.reloadableHoconConfig(vertx, classpathDefaultsPath = "emptyJson.json")
        assertFails {
            vertx.deployVerticle(MyVerticle()).await()
        }
    }

    "Suceeds on deploy if a valid config is given as the override" {
        val configFile = tempfile().also { it.writeText("""{ "foo": "success" }""") }
        ConfigurableCoroutineVerticle.reloadableHoconConfig(vertx, overrideFileLocation = configFile.absolutePath)
        val verticle = MyVerticle().also {
            vertx.deployVerticle(it).await()
        }
        verticle.foo shouldBe "success"
    }

    "Suceeds on deploy if a valid config is given as the default" {
        ConfigurableCoroutineVerticle.reloadableHoconConfig(vertx, classpathDefaultsPath = "fooJson.json")
        val verticle = MyVerticle().also {
            vertx.deployVerticle(it).await()
        }
        verticle.foo shouldBe "bar"
    }

    "Suceeds on deploy if a valid config is given via deployment options only" {
        val verticle = MyVerticle().also {
            vertx.deployVerticle(it, deploymentOptionsOf(config = JsonObject().put("foo", "gummybar"))).await()
        }
        verticle.foo shouldBe "gummybar"
    }

    "Configuration via deployment options will override the default config" {
        ConfigurableCoroutineVerticle.reloadableHoconConfig(vertx, classpathDefaultsPath = "fooJson.json")
        val verticle = MyVerticle().also {
            vertx.deployVerticle(it, deploymentOptionsOf(config = JsonObject().put("foo", "not bar"))).await()
        }
        verticle.foo shouldBe "not bar"
    }

    "Having a file backed config resolver" - {
        val configFile = tempfile().also { it.writeText("""{ "foo": "initial" }""") }
        val retriever = ConfigurableCoroutineVerticle.reloadableHoconConfig(vertx, overrideFileLocation = configFile.absolutePath)
        val verticle = MyVerticle().also {
            vertx.deployVerticle(it).await()
        }

        "a deployed verticle will read its configuration after the config changes" {
            verticle.foo shouldBe "initial"
            configFile.writeText("""{ "foo": "overwritten" }""")
            eventually(15.seconds) {
                verticle.foo shouldBe "overwritten"
            }
        }
    }
})
