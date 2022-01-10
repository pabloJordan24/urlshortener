package es.unizar.urlshortener.infrastructure.delivery

/*
import org.springframework.http.MediaType
import java.io.BufferedReader
import java.io.File
import java.io.IOException*/
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
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
import java.net.URI
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
    //fun handleCsvUpload(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource>
    fun handleCsvUpload(request: HttpServletRequest): ResponseEntity<String>
}


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlCsvShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createCsvShortUrlUseCase: CreateCsvShortUrlUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase
) : UrlCsvShortenerController {

    /*@PostMapping("/csv", consumes = [ "multipart/form-data" ], produces=["text/csv"])
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
    }*/
    
    @PostMapping("/csv")
    override fun handleCsvUpload(request: HttpServletRequest): ResponseEntity<String> {
        // Abrir servidor WebSockets
        return ResponseEntity<String>(request.remoteAddr, HttpStatus.ACCEPTED)
    }
}