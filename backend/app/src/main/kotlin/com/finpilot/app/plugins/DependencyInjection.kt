package com.finpilot.app.plugins

import com.finpilot.app.config.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    val appConfig = AppConfig.from(environment.config)
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single { appConfig }
            },
        )
    }
}
