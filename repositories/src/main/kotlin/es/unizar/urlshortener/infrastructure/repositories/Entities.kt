package es.unizar.urlshortener.infrastructure.repositories

import java.awt.image.BufferedImage
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.*

/**
 * The [ClickEntity] entity logs clicks.
 */
@Entity
@Table(name = "click")
class ClickEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long?,
    val hash: String,
    val created: OffsetDateTime,
    val ip: String?,
    val referrer: String?,
    val browser: String?,
    val platform: String?,
    val country: String?
)

/**
 * The [ShortUrlEntity] entity stores short urls.
 */
@Entity
@Table(name = "shorturl")
class ShortUrlEntity(
    @Id
    val hash: String,
    val target: String,
    val sponsor: String?,
    val created: OffsetDateTime,
    val owner: String?,
    val mode: Int,
    val safe: Boolean,
    val ip: String?,
    val country: String?
)

/**
 * The [QRCodeEntity] entity stores short urls.
 */
@Entity
@Table(name = "qrcode")

class QRCodeEntity(
    @Id
    val hash: String,
    val urlhash: String,
    @Column(name="byteqr", length = 100000)
    val byteqr:ByteArray? = null,

    val created: OffsetDateTime,

)