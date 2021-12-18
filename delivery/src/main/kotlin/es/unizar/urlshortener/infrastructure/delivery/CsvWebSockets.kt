package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.NotReachableException
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import java.net.NetworkInterface
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import javax.servlet.http.HttpServletRequest
import javax.websocket.*
import javax.websocket.server.ServerEndpoint


object DI {
    lateinit var context: ApplicationContext
    fun register(applicationContext: ApplicationContext) {
        context = applicationContext
    }
    inline fun <reified T> bean(): Lazy<T> {
        return lazy { context.getBean(T::class.java) ?: throw NullPointerException() }
    }
}

@Configuration
@EnableWebSocket
class WebSocketConfig {
    @Bean
    fun serverEndpointExporter(): ServerEndpointExporter = ServerEndpointExporter()
}

@ServerEndpoint("/csv/progress")
@Component
class CsvEndpoint() {

    private val createShortUrlUseCase: CreateShortUrlUseCase by DI.bean()

    @OnOpen
    fun onOpen(session: Session) {
        println("Server Connected ... Session ${session.id}")
            with(session.asyncRemote) {
                sendText("Send me the URLs")
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
        if (message != "There are no more URLs") {
                with(session.asyncRemote) {
                    sendText(shortener(message))
                }
        } else {
            session.close(CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"))
        }
    }

    @OnError
    fun onError(session: Session, errorReason: Throwable) {
        println("Session ${session.id} closed because of ${errorReason.javaClass.name}")
    }

    fun shortener(originalUrl: String): String {
        try {
            createShortUrlUseCase.create(
                url = originalUrl,
                data = ShortUrlProperties(
                    ip = "0:0:0:0:0:0:0:1",
                    sponsor = null
                )
            ).let {
                //val url = linkTo<UrlCsvShortenerControllerImpl> { redirectCsvTo(it.hash, "0:0:0:0:0:0:0:1") }.toUri()
                val url = "http://localhost:8080/tiny-${it.hash}"
                return "$originalUrl,$url";
            }
        }
        catch(e: InvalidUrlException) {
            println(e.message.toString())
            //original_shortened_map[line] = e.message.toString()
            return "$originalUrl,${e.message.toString()}";
        }
        catch(e: NotReachableException) {
            println(e.message.toString())
            //original_shortened_map[line] = e.message.toString()
            return "$originalUrl,${e.message.toString()}";
        }
    }
}