package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickRepositoryService
import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlRepositoryService
import es.unizar.urlshortener.core.*
import java.time.OffsetDateTime

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(private val clickEntityRepository: ClickEntityRepository) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()
    override fun showAll(): List<Click> = clickEntityRepository.findAll().map {it.toDomain()}
    override fun countByHash(hash: String): Int  = clickEntityRepository.findAllByHash(hash).count()
    override fun fetchIPClient(hash: String, ofsdt: OffsetDateTime) : List<String?> = clickEntityRepository.fetchByHash(hash,ofsdt).map {it}
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(private val shortUrlEntityRepository: ShortUrlEntityRepository) : ShortUrlRepositoryService {
    //shortUrlEntityRepository es un repo que hereda de JpaRespository y que ya tiene una serie de funciones
    //predeterminadas, como pueden ser save, saveAll(), etc.  
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    //esta función se podría haber llamado addShortUrl para diferenciarla de la función save, una
    //de las que nos ofrece JPArepository para contactar con BD. 
    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()

    //showAll() me devuelve una lista con todas las tuplas del repo de shortUrls.
    override fun showAll(): List<ShortUrl> = shortUrlEntityRepository.findAll().map {it.toDomain()}
}

/**
 * Implementation of the port [QRCodeRepositoryService].
 */
class QRCodeRepositoryServiceImpl(
    private val qrcodeEntityRepository: QRCodeEntityRepository
) : QRCodeRepositoryService {
    override fun findByKey(id: String): QRCode? = qrcodeEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: QRCode): QRCode = qrcodeEntityRepository.save(su.toEntity()).toDomain()
}
