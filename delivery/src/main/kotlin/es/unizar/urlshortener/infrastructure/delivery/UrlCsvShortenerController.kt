package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest


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

    fun downloadCSV(): ResponseEntity<Resource>

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
        val nameShortenedFile = "Shortened_" + file.originalFilename
        val shortenedFile = File(nameShortenedFile)
        shortenedFile.writeText("")
        val h = HttpHeaders()
        var status = 400
        var headerLocationCreado = false
        map.forEach { 
            //si es un hash y no un mensaje de error, le aplico la redireccion
            if (it.value.length <= 8) {
                val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.value, request) }.toUri()
                //write the first URL in "Location" header
                if(!headerLocationCreado) {
                    headerLocationCreado = true
                    h.add("Location", url.toString())
                    status = 201
                }
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

        //Falta eliminar el fichero para liberar espacio en el server o crearlo temporal

        return ResponseEntity.status(status)
            .header("Content-Disposition", "attachment; filename=\"" + nameShortenedFile + "\"")
            .headers(h)
            .contentLength(shortenedFile.length())
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource)
    }

    @RequestMapping(path = ["/csv/download"], method = [RequestMethod.GET])
    @Throws(IOException::class)
    override fun downloadCSV(): ResponseEntity<Resource> {

        val shortenedFile = File("shortened.csv")
        val h = HttpHeaders()
        val path: Path = Paths.get(shortenedFile.getAbsolutePath())
        val resource = ByteArrayResource(Files.readAllBytes(path))
        return ResponseEntity.ok()
            .headers(h)
            .contentLength(shortenedFile.length())
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(resource)
    }
}