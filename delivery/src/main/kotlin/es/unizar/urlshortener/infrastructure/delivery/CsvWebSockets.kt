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

@Configuration
@EnableWebSocket
class WebSocketConfig {
    @Bean
    fun serverEndpoint() = ServerEndpointExporter()
}

@ServerEndpoint("/csv/progress")
@Component
class CsvEndpoint(val createShortUrlUseCase: CreateShortUrlUseCase) {
    @OnOpen
    fun onOpen(session: Session) {
        println("Server Connected ... Session ${session.id}")
        synchronized(session) {
            with(session.basicRemote) {
                sendText("Send me the URLs")
            }
        }
    }

    @OnClose
    fun onClose(session: Session, closeReason: CloseReason) {
        println("Session ${session.id} closed because of $closeReason")
    }

    @OnMessage
    fun onMsg(message: String, session: Session) {
        println("Server Message ... Session ${session.id}")
        println("Server received $message")
        if (message != "That was the last URL") {
            synchronized(session) {
                with(session.basicRemote) {
                    //sendText(shortener(message))
                }
            }
        } else {
            session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"))
        }
    }

    @OnError
    fun onError(session: Session, errorReason: Throwable) {
        println("Session ${session.id} closed because of ${errorReason.javaClass.name}")
    }

    fun shortener(originalUrl: String, request: HttpServletRequest): String {
        createShortUrlUseCase.create(
            url = originalUrl,
            data = ShortUrlProperties(
                ip = "remoteAddr",
                sponsor = null
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }
        return "url acortada";
    }
}