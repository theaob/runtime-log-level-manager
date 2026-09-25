package io.github.theaob.loglevel

import io.github.theaob.loglevel.backend.JulBackend
import io.github.theaob.loglevel.backend.Log4j2Backend
import io.github.theaob.loglevel.backend.LogbackBackend
import java.util.ServiceLoader

/** Detects which logging framework the application is using. */
object LoggingBackends {

    /**
     * Picks a backend in this order: a [ServiceLoader] registered [LoggingBackend], the framework
     * SLF4J is bound to, Log4j2 core, and finally java.util.logging which is always available.
     */
    @JvmStatic
    fun detect(): LoggingBackend {
        ServiceLoader.load(LoggingBackend::class.java).firstOrNull()?.let { return it }

        val slf4jFactory = slf4jFactoryClassName()
        when {
            slf4jFactory == "ch.qos.logback.classic.LoggerContext" -> return LogbackBackend()
            slf4jFactory == "org.slf4j.jul.JDK14LoggerFactory" || slf4jFactory == "org.slf4j.impl.JDK14LoggerFactory" ->
                return JulBackend()
        }
        if (isLog4j2CoreActive()) return Log4j2Backend()
        return JulBackend()
    }

    private fun slf4jFactoryClassName(): String? = runCatching {
        Class.forName("org.slf4j.LoggerFactory").getMethod("getILoggerFactory").invoke(null).javaClass.name
    }.getOrNull()

    private fun isLog4j2CoreActive(): Boolean = runCatching {
        val coreContext = Class.forName("org.apache.logging.log4j.core.LoggerContext")
        val context = Class.forName("org.apache.logging.log4j.LogManager")
            .getMethod("getContext", Boolean::class.javaPrimitiveType)
            .invoke(null, false)
        coreContext.isInstance(context)
    }.getOrDefault(false)
}
