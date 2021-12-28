package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.*
import javax.servlet.http.HttpServletRequest
import kotlin.concurrent.thread


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

    var colaQR: LinkedBlockingQueue<Runnable>

    var pool: ThreadPoolExecutor
    var inicio: ThreadPoolExecutor

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



//Crea el QR
class TareaCrearQR (
    val urlhash: String,
    val qrhash: String,
    val qrGeneratorUseCase: QRGeneratorUseCase,
) : Runnable {
    override fun run() {
        //meter aquí en vez del sleep el servicio de comprobar la alcanzabilidad
        Thread.sleep(5_000)
        qrGeneratorUseCase.create(urlhash,qrhash)
    }
}

//Lee la cola y ejecuta la tarea leída
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
    val qrGeneratorUseCase: QRGeneratorUseCase,
    val qrImageUseCase: QRImageUseCase,
    val createQRURLUseCase: CreateQRURLUseCase,
) : UrlShortenerController {

    override var colaQR = LinkedBlockingQueue<Runnable>()
    override var inicio = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, colaQR)
    override var pool = ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, colaQR)

    init{
        inicio.execute(ManageQueue(pool,colaQR))
    }
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
                //CREAMOS EL HASH DEL QR PUES NO SUPONE MUCHO COSTE COMPUTACIONAL
                var qrhash = createQRURLUseCase.create(
                    data = it.hash
                )
                uriqr = linkTo<UrlShortenerControllerImpl> { redirectQr(qrhash, request) }.toUri()
                h.set("qr", uriqr.toString());
                //METO TAREA CREAR QR EN LA COLA
                println("tiny-"+it.hash)
                val tareaQR = TareaCrearQR(it.hash,qrhash, qrGeneratorUseCase)
                colaQR.put(tareaQR)

                /*Crea el QR asociado a ese HASH
                thread {
                    Thread.sleep(10000)
                    qrGeneratorUseCase.create(
                        data = it.hash
                    )
                    println("FIN")
                }*/

            }

            val response = ShortUrlDataOut(
                url = url,
                properties = mapOf(
                    "safe" to it.properties.safe
                ),
                qr = uriqr
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
            /*println("ESPERANDO")
            Thread.sleep(10000)
            println("FIN ESPERANDO")
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)*/

        }

    @GetMapping("/qrcode-{id:.*}" , produces = [MediaType.IMAGE_JPEG_VALUE])
    override fun redirectQr(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray>{
        qrImageUseCase.image(id).let {
            val h = HttpHeaders()
            return ResponseEntity<ByteArray>(it.qr, h, HttpStatus.OK)
        }

    }


}