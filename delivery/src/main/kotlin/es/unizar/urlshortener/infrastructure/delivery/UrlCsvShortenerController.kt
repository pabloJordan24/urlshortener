package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest
import java.io.BufferedReader
import org.springframework.hateoas.server.mvc.linkTo

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
    fun handleCsvUpload(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource>

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

    @PostMapping("/csv", consumes = [ "multipart/form-data" ], produces=["text/csv"])
    override fun handleCsvUpload(@RequestParam("csv") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource> {
        //leemos los bytes del multipart
        val reader: BufferedReader = file.getInputStream().bufferedReader()

        //creamos sólo las shortUrls ó en su defecto, conocemos el error
        val map = createCsvShortUrlUseCase.create(reader,request.remoteAddr)

        //creamos el fichero de salida con el resultado
        val shortenedFile = File("shortened.csv")
        shortenedFile.writeText("")

        map.forEach { 
            //si es un hash y no un mensaje de error, le aplico la redireccion
            if (it.value.length <= 8) {
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.value, request) }.toUri()
                
                //write original URI and shortened one
                val valorEscribir = it.key + "," + url + "\n";
                shortenedFile.appendText(valorEscribir)
                //println("Clave: "+it.key+", Valor: "+url)
            }
            else {
                //write original URI and shortened one
                val valorEscribir = it.key + ", ," + it.value + "\n";
                shortenedFile.appendText(valorEscribir)
                //println("Clave: "+it.key+", Valor: "+it.value)
            }
        }

        val path: Path = Paths.get(shortenedFile.absolutePath)
        val resource = ByteArrayResource(Files.readAllBytes(path))
        val h = HttpHeaders()

        return ResponseEntity.ok()
            .headers(h)
            .contentLength(shortenedFile.length())
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource)
    }
}