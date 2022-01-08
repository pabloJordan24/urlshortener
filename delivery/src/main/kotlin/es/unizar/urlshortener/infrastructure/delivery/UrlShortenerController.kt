package es.unizar.urlshortener.infrastructure.delivery

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import es.unizar.urlshortener.core.usecases.*
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

    /**
     * Redirects to image QR identified by its [id].
     *
     * **Note**: Delivery of use case [QRImageUseCase].
     */
    fun redirectQr(id: String, request: HttpServletRequest): ResponseEntity<ByteArray>

}

/*
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean
)

/*
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qr:  URI? = null
)

/*
 * Data returned after getting the info of a shortened URL.
 */
data class ShortUrlInfoData(
    val numClicks: Int,
    val creationDate: String,
    val uriDestino: URI,
    val usersClicks: List<String?>
)


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
    val alcanzableUseCase: AlcanzableUseCase,
    val qrImageUseCase: QRImageUseCase,
    val qrGeneratorUseCase: QRGeneratorUseCase,
    val createQRURLUseCase: CreateQRURLUseCase
) : UrlShortenerController {

    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                safe="not validated"
            )
        ).let {
            val userAgent = request.getHeader("User-Agent")
            println(userAgent)
            
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            
            var response: ShortUrlDataOut? = null

            if (data.qr) {
                //creamos el hash del qr porque no supone mucho coste computacional
                var qrhash = createQRURLUseCase.create(
                    data = it.hash
                )
                var uriqr = linkTo<UrlShortenerControllerImpl> { redirectQr(qrhash, request) }.toUri()
                h.set("qr", uriqr.toString());

                response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "not validated" to it.properties.safe
                    ),
                    qr = uriqr
                )
            }
            else {
                response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "not validated" to it.properties.safe
                    )
                )
            }

            //devolvemos 202
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.ACCEPTED)
        }

    @GetMapping("/{hash}.json")
    override fun getURLinfo(@PathVariable hash: String): ResponseEntity<ShortUrlInfoData> {
        infoShortUrlUseCase.showStats(hash).let {
            val h = HttpHeaders()
            val response = ShortUrlInfoData (numClicks = it.clicks, creationDate=it.created, uriDestino=URI.create(it.uri),
            usersClicks=it.users)
            return ResponseEntity<ShortUrlInfoData>(response, h, HttpStatus.OK)
        }
    }

    @GetMapping("/qrcode-{id:.*}" , produces = [MediaType.IMAGE_JPEG_VALUE])
    override fun redirectQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray>{
        qrImageUseCase.image(id).let {
            val h = HttpHeaders()
            return ResponseEntity<ByteArray>(it.qr, h, HttpStatus.OK)
        }
    }
}
