package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import es.unizar.urlshortener.core.ReachableService
import es.unizar.urlshortener.core.QRService

import java.nio.charset.StandardCharsets
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import javax.imageio.ImageIO
import java.io.File

import io.github.g0dkar.qrcode.QRCode


import org.apache.commons.validator.routines.UrlValidator
import org.springframework.scheduling.annotation.Async


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
  * Implementation of the port [ReachableService].
  */
class ReachableServiceImpl : ReachableService {
    //@Async
    override fun isReachable(url: String): Boolean {
        val auxUrl: URL = URL(url)
        val connection: HttpURLConnection = auxUrl.openConnection() as HttpURLConnection
        connection.setConnectTimeout(5000)
        connection.setRequestMethod("GET")
        connection.connect()
        val code = connection.getResponseCode()
        println("Devuelvooooo: "+code)

        return (code==200)
    }
 }

/**
 * Implementation of the port [QRService].
 */
class QRServiceImpl : QRService {
    override fun qr(url: String) = QRCode(url).render()
    override fun qrbytes(img: BufferedImage) : ByteArray {
        ImageIO.write(img, "PNG", File("qri.png"))
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val bytes = baos.toByteArray()
        
        return  bytes
    }
}