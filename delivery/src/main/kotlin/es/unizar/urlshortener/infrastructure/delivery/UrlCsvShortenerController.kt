package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest
import javax.websocket.*
import javax.websocket.server.ServerEndpoint


/**
 * The specification of the controller.
 */
interface UrlCsvShortenerController {
    /**
     * Recieves a CSV file with URIs to shorten.
     *
     * Returns another csv file with the URIs shortened (if possible). If not,
     * a message specifying the error is shown.
     */
    fun handleCsvUpload(request: HttpServletRequest): ResponseEntity<String>

}


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlCsvShortenerControllerImpl(
    val createCsvShortUrlUseCase: CreateCsvShortUrlUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase
) : UrlCsvShortenerController {

    @PostMapping("/csv")
    override fun handleCsvUpload(request: HttpServletRequest): ResponseEntity<String> {
        // Abrir servidor WebSockets
        return ResponseEntity<String>("Servidor WebSockets esperando... ", HttpStatus.ACCEPTED)
    }
}