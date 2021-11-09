package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.QRService
import es.unizar.urlshortener.core.ValidatorService
import io.github.g0dkar.qrcode.QRCode
import org.apache.commons.validator.routines.UrlValidator
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32().hashString(url, StandardCharsets.UTF_8).toString()
}

/**
 * Implementation of the port [QRService].
*/
class QRServiceImpl : QRService {

    override fun qr(url: String)= QRCode(url).render()
    override fun qrbytes(img: BufferedImage): ByteArray {
        ImageIO.write(img, "PNG", File("qri.png"))
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val bytes = baos.toByteArray()
        /* val baos2 = ByteArrayOutputStream(bytes.size)
         baos2.write(bytes, 0, bytes.size)
         println("?????????????: " +baos2)
         val inputStream: InputStream = File("/Users/diegogarcia/Desktop/unicuarto/iw/trabajo/urlshortener-master/app/qr.txt").inputStream()
         val inputString = inputStream.bufferedReader().use { it.readText() }*/
        return  bytes
    }


}