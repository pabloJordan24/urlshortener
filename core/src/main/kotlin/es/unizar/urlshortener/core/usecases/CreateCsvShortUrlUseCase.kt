package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest


/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateCsvShortUrlUseCase {
    fun create(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource>
}

class CreateCsvShortUrlUseCaseImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val redirectUseCase: RedirectUseCase
): CreateCsvShortUrlUseCase {
    override fun create(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource> {
        val filepath: Path = Paths.get(file.originalFilename)
        Files.newOutputStream(filepath).use { os -> os.write(file.bytes) }
        val shortenedFile = File("shortened.csv")
        try {
            val fileName = "ejemplo.csv"
            shortenedFile.writeText("")
            var lines:List<String> = File(fileName).readLines()
            lines.forEach {
                    line -> println(line)
                    createShortUrlUseCase.create(
                        url = line,
                        data = ShortUrlProperties()
                    ).let {
                        shortenedFile.appendText("$line,$it\n")
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
}