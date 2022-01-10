package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long> {
    fun findAllByHash(hash: String) : List<ClickEntity>
    //@Query("SELECT ip FROM click WHERE (click.created <= ?2 and click.hash=?1) GROUP BY ip", nativeQuery=true)
    @Query("SELECT DISTINCT ip FROM click WHERE click.hash=?1 and click.created>=?2", nativeQuery=true)
    fun fetchByHash(hash: String, ofsdt: OffsetDateTime) : List<String?>
}

/**
 * Specification of the repository of [QRCodelEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface QRCodeEntityRepository : JpaRepository<QRCodeEntity, String> {
    fun findByHash(hash: String): QRCodeEntity?
}