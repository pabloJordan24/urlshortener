package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.awt.image.BufferedImage
import java.net.URI
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest
import javax.swing.text.html.ImageView


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

    fun redirectQr(id: String, request: HttpServletRequest): ResponseEntity<ByteArray>

}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val qr: Boolean

)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qr:  URI? = null,

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
    val qrGeneratorUseCase: QRGeneratorUseCase,
    val qrImageUseCase: QRImageUseCase
) : UrlShortenerController {



    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            println(id)
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

//Cuando hace post ahi se hace shortener
@PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            println(data)
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url


            var uriqr: URI? = null

            if (data.qr){

                var q = qrGeneratorUseCase.create(
                    data = it.hash
                )

                uriqr = linkTo<UrlShortenerControllerImpl> { redirectQr(q.qrhash, request) }.toUri()
                h.set("qr", uriqr.toString());

            }

            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                ),
                qr = uriqr
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/qrcode-{id:.*}" , produces = [MediaType.IMAGE_JPEG_VALUE])
    override fun redirectQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray>{
        qrImageUseCase.image(id).let {
            val h = HttpHeaders()
            return ResponseEntity<ByteArray>(it.qr, h, HttpStatus.OK)
        }

    }


}



