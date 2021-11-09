package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
//import es.unizar.urlshortener.core.usecases.QRGeneratorUseCase
import io.github.g0dkar.qrcode.QRCode
import org.springframework.core.io.ByteArrayResource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.time.OffsetDateTime
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest

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

    fun redirectQr(id: String, request: HttpServletRequest): ResponseEntity<QRInfoData>
    fun checkingqr(data: CheckDataIn, request: HttpServletRequest): ResponseEntity<CheckDataIn>
    //fun prueba(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<String>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    //val qr : Boolean
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap(),
    val qr:  URI? = null,
)

data class QRInfoData(
    val qrhash: String,
    val ShortUrlhash: String,
    val qr: ByteArray,
    val created: OffsetDateTime
)
data class CheckDataIn(
    val isCheck: String,
    val qr:  URI? = null,
)

var envioQR: Boolean = false

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


            if (envioQR){
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
            println("ENVIAMOS ????" +envioQR)
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/qrcode-{id:.*}")
    override fun redirectQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<QRInfoData>{
        qrImageUseCase.image(id).let {
            println("ENTRAAAA: "+id)
           /* val baos2 = ByteArrayOutputStream(it.qr.size)
            baos2.write(it.qr, 0, it.qr.size)*/
            val response = QRInfoData(qrhash = it.qrhash, ShortUrlhash= it.ShortUrlhash , qr = it.qr, created = it.created)

            val h = HttpHeaders()
            return ResponseEntity<QRInfoData>(response, h, HttpStatus.OK)
        }

    }

    //Cuando hace post ahi se hace shortener
    @PostMapping("/api/checkqr", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    override fun checkingqr(data: CheckDataIn, request: HttpServletRequest): ResponseEntity<CheckDataIn> {
        println(data.isCheck)
        if (data.isCheck == "true"){
            envioQR = true
        }else{
            envioQR = false
        }

        val h = HttpHeaders()
        val response = CheckDataIn(isCheck = "bien")
        return ResponseEntity<CheckDataIn>(response, h, HttpStatus.OK)
    }

}



