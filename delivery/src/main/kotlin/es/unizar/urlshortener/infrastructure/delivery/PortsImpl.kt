package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import es.unizar.urlshortener.core.ReachableService
import es.unizar.urlshortener.core.QRService
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import io.github.g0dkar.qrcode.QRCode
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection

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

/*
 * Implementation of the port [QRService].
 */
class QRServiceImpl : QRService {
    override fun qr(url: String) : String {

        val imageData = QRCode("https://github.com/g0dkar/qrcode-kotlin").render()

        return "hola"
    }
}

/**
  * Implementation of the port [ReachableService].
  */
class ReachableServiceImpl : ReachableService {
    override fun isReachable(url: String): Boolean {
        val auxUrl: URL = URL(url)
        val connection: HttpURLConnection = auxUrl.openConnection() as HttpURLConnection
        connection.setConnectTimeout(5000)
        connection.connect()
        val code = connection.getResponseCode()
        println(code)

        if (code == 200) { 
            return true
        } else{
            return false
        }
    }
 }