package si.razum.vertx.db

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * The database configuration class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DbConfig(
    /** The host/URL at which the DB is located */
    val host: String,

    /** The port on which the DB is listening */
    val port: Int = 5432,

    /** The name of the database */
    val database: String,

    /** The name of the user for this database */
    val user: String,

    /** The password used to access this database */
    val password: String? = null,

    /** When non-null, the database will connect to this socket rather than via network */
    val socketFile: String? = null,
)
