package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.DI
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

/**
 * The marker that makes this project a Spring Boot application.
 */
@SpringBootApplication
class UrlShortenerApplication

/**
 * The main entry point.
 */
fun main(args: Array<String>) {
    DI.register(runApplication<UrlShortenerApplication>(*args))
}
