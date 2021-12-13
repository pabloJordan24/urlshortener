package es.unizar.urlshortener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.*
import org.springframework.scheduling.concurrent.*

import java.util.concurrent.*

/**
 * The marker that makes this project a Spring Boot application.
 */
@SpringBootApplication
@EnableScheduling
class UrlShortenerApplication 

/**
 * The main entry point.
 */

fun main(args: Array<String>) {
    runApplication<UrlShortenerApplication>(*args)
}

