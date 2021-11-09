package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
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
