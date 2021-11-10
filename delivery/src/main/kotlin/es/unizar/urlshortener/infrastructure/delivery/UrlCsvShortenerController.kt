package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.CreateCsvShortUrlUseCase
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest


interface UrlCsvShortenerController {

    fun handleCsvUpload(file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource>

}

@RestController
class UrlCsvShortenerControllerImpl(
    val createCsvShortUrlUseCase: CreateCsvShortUrlUseCase
) : UrlCsvShortenerController {

    @PostMapping("/csv", consumes = [ "multipart/form-data" ])
    override fun handleCsvUpload(@RequestParam("uploadCSV") file: MultipartFile, request: HttpServletRequest): ResponseEntity<Resource> {
        createCsvShortUrlUseCase.create(
            file = file,
            request = request
        ).let {
            return it
        }
    }

    /*@PostMapping("/csv", consumes = [ "multipart/form-data" ])
    override fun handleCsvUpload(@RequestParam("uploadCSV") file: MultipartFile): ResponseEntity<String> {
        try {

            // upload directory - change it to your own
            val UPLOAD_DIR = ""

            // create a path from file name
            val path: Path = Paths.get(UPLOAD_DIR, file.originalFilename)

            // save the file to `UPLOAD_DIR`
            // make sure you have permission to write
            Files.write(path, file.bytes)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ResponseEntity("Invalid file format!!", HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity("File uploaded!!", HttpStatus.OK)
    }*/
}