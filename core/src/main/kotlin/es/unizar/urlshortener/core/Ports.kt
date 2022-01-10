package es.unizar.urlshortener.core
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
    fun showAll(): List<Click>
    //devuelve los clicks a ese hash
    fun countByHash(hash: String): Int 
    fun fetchIPClient(hash: String, ofsdt: OffsetDateTime) : List<String?>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    //se podría haber llamado addShortUrl para diferenciarlo un poco del save que nos da el JPArepository
    fun save(su: ShortUrl): ShortUrl
    fun showAll(): List<ShortUrl>
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

/**
  * [ReachableService] is the port to the service that checks if an url is reachable so it can be shortened.
  *
  * **Note**: It is a design decision to create this port. It could be part of the core .
  */
 interface ReachableService {
    fun isReachable(url: String): Boolean
 } 


 /**
 * [QRCodeRepositoryService] is the port to the repository that provides management to [QRCode][QRCode].
 */
interface QRCodeRepositoryService {
    fun findByKey(id: String): QRCode?
    fun save(su: QRCode): QRCode
}

/**
 * [QRService] is the port to the service that creates a QR code from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface QRService {
    fun qr(url: String): BufferedImage
    fun qrbytes(img: BufferedImage): ByteArray
}