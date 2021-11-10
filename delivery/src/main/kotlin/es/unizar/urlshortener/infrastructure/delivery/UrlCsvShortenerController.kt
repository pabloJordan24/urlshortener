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
import es.unizar.urlshortener.infrastructure.delivery.UrlShortenerControllerImpl


interface UrlCsvShortenerController {

    fun handleCsvUpload(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource>

}

@RestController
class UrlCsvShortenerControllerImpl(
    val createCsvShortUrlUseCase: CreateCsvShortUrlUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase
) : UrlCsvShortenerController {

    /*@PostMapping("/csv", consumes = [ "multipart/form-data" ])
    override fun handleCsvUpload(@RequestParam("uploadCSV") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource> {
        createCsvShortUrlUseCase.create(
            file = file,
            request = request
        ).let {
            return it
        }
    }*/

    @PostMapping("/csv", consumes = [ "multipart/form-data" ])
    override fun handleCsvUpload(@RequestParam("uploadCSV") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource> {
        val filepath: Path = Paths.get(file.originalFilename)
        Files.newOutputStream(filepath).use { os -> os.write(file.bytes) }
        val shortenedFile = File("shortened.csv")
        try {
            shortenedFile.writeText("")
            var lines:List<String> = File(file.originalFilename).readLines()
            lines.forEach {
                    line -> println(line)
                createShortUrlUseCase.create(
                    url = line,
                    data = ShortUrlProperties(
                        ip = request.remoteAddr
                    )
                ).let {
                    val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
                    println(url)
                    shortenedFile.appendText("$line,$url\n")
                }
            }

        } catch (e:Exception) {
            e.printStackTrace()
        } finally {
            println("__________FINISHED__________")
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

    /*@PostMapping("/csv", consumes = [ "multipart/form-data" ])
    override fun handleCsvUpload(@RequestParam("uploadCSV") file: MultipartFile, request: HttpServletRequest): ResponseEntity<String> {
        val filepath: Path = Paths.get(file.originalFilename)
        Files.newOutputStream(filepath).use { os -> os.write(file.bytes) }
        try {
            val fileName = "ejemplo.csv"
            val shortenedFile = File("shortened.csv")
            shortenedFile.writeText("")
            var lines:List<String> = File(fileName).readLines()
            lines.forEach {
                    line -> println(line)
                val url = linkTo<UrlShortenerControllerImpl> { shortener(ShortUrlDataIn(line), request) }
                shortenedFile.appendText("$line,$it\n")
            }
        } catch (e:Exception) {
            e.printStackTrace()
        } finally {
            println("__________FINISHED__________")
        }
    }*/
}