package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.awt.image.BufferedImage
import java.io.File
import java.time.OffsetDateTime
import java.util.Date

/**
 *
 *
 * **Note**: This is an example of functionality.
 */

interface QRGeneratorUseCase {
    fun create(data: String,): QRCode
}

/**
 * Implementation of [QRGeneratorUseCase].
 */
class QRGeneratorUseCaseImpl(
    private val qrCodeRepository: QRCodeRepositoryService,
    private val qrService: QRService,
    private val hashService: HashService
) : QRGeneratorUseCase {
    override fun create(data: String): QRCode {

        var qrc = qrService.qr("http://localhost/" + data)
        val qrCode = QRCode(
            qrhash = hashService.hasUrl("http://localhost/" + data),
            ShortUrlhash = data,
            qr = qrService.qrbytes(qrc)
        )




        qrCodeRepository.save(qrCode)
        println("LEE FICHERO: " +qrCode.qr)
        return qrCode
    }
}
