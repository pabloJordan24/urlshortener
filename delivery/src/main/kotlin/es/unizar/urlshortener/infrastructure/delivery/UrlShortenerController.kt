package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.AlcanzableUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.InfoShortUrlUseCase
import es.unizar.urlshortener.core.InvalidUrlException
import es.unizar.urlshortener.core.NotReachableException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.concurrent.*

import java.net.URI
import javax.servlet.http.HttpServletRequest
import java.time.OffsetDateTime
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.lang.Runnable
import mu.KotlinLogging

import java.util.concurrent.*

/**
 * The specification of the controller.
 */
interface UrlShortenerController {
    
    /* Thread Pool: https://developer.android.com/reference/kotlin/java/util/concurrent/Executors */
    val executor: Executor

    /* Task queue (in this case, it only proves if shortUrl is reachable). 
     * It is defined as String type because we only need the URL´s key.
     */
    var colaAlcanzabilidad: LinkedBlockingQueue<Runnable>

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Dumps info about short url identified by its [id].
     *
     * **Note**: Delivery of use case [GetInfoShortenedURL].
     */
    fun getURLinfo(hash: String): ResponseEntity<ShortUrlInfoData>

}

/*
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null
)

/*
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/*
 * Data returned after getting the info of a shortened URL.
 */
data class ShortUrlInfoData(
    val numClicks: Int,
    val creationDate: String,
    val uriDestino: URI
)

/*
 * Function that returns an executor (thread pool).
 */
fun taskExecutor(mycorePoolSize:Int, mymaxPoolSize:Int): Executor {
    val executor = ThreadPoolTaskExecutor() 
    executor.corePoolSize = mycorePoolSize
    executor.maxPoolSize = mymaxPoolSize 
    executor.setQueueCapacity(500) 
    executor.setThreadNamePrefix("Task-") 
    executor.initialize()
    return executor
}

//meter la concurrentqueue con el thread que lea de la cola

class TareaComprobarUrlAlcanzable (
    val key: String, 
    val alcanzableUseCase: AlcanzableUseCase
    ) : Runnable {
    override fun run() {
        val logger = KotlinLogging.logger {}
        //meter aquí en vez del sleep el servicio de comprobar la alcanzabilidad
        Thread.sleep(5_000)
        alcanzableUseCase.esAlcanzable(key)
        logger.info { "Tarea acabada" }
    }
}

class ManageQueue (
    val executor: Executor,
    var cola: LinkedBlockingQueue<Runnable>
) : Runnable {
    override fun run() {
        while(true) {
            val t = cola.take()
            executor.execute(t)
        }
    }
}

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val infoShortUrlUseCase: InfoShortUrlUseCase,
    val alcanzableUseCase: AlcanzableUseCase
) : UrlShortenerController {
    // Me hago un pool de threads fijo. aquellas peticiones que no se puedan atender,
    // se encolan para que luego otro thread las pille. 
    // En vd, esto es lo mismo que usar la bean de spring => ThreadPoolTaskExecutor()
    //override val executor = Executors.newFixedThreadPool(2)

    override val executor = taskExecutor(2,2)

    override var colaAlcanzabilidad = LinkedBlockingQueue<Runnable>()

    init {
        //lanzamos un thread que inicialice nuestra cola y al thread pool que 
        //lee de ella.
        var lanzador = taskExecutor(1,1)
        lanzador.execute(ManageQueue(executor,colaAlcanzabilidad))
    }

    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            //println("redirecttttt")
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                safe=false
            )
        ).let {
            //println(it.hash)
            val userAgent = request.getHeader("User-Agent")
            println(userAgent)
            
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                )
            )

            //lanzo tarea de comprobar la alcanzabilidad de la url devuelta
            val tarea = TareaComprobarUrlAlcanzable(it.hash,alcanzableUseCase)
            //executor.execute(tarea)
            //en el nivel 15 -> encolamos la tarea (con el hash a comprobar).
            colaAlcanzabilidad.put(tarea)

            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/{hash}.json")
    override fun getURLinfo(@PathVariable hash: String): ResponseEntity<ShortUrlInfoData> {
        infoShortUrlUseCase.showStats(hash).let {
            val h = HttpHeaders()
            val response = ShortUrlInfoData (numClicks = it.clicks, creationDate=it.created, uriDestino=URI.create(it.uri))
            return ResponseEntity<ShortUrlInfoData>(response, h, HttpStatus.OK)
        }
    }
}
