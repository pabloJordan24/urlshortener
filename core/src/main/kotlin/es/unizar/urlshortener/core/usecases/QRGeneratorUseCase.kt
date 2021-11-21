package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

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
        println("VAMOS A ENVIAR ???? "+data)


        var qrc = qrService.qr("http://localhost/" + data)
        var hashqr = hashService.hasUrl("http://localhost/" + data)
        var qrCode = qrCodeRepository.findByKey(hashqr)

        if(qrCode == null){
            println("NO EXISTE")
            qrCode = QRCode(
                qrhash = hashService.hasUrl("http://localhost/" + data),
                ShortUrlhash = data,
                qr = qrService.qrbytes(qrc)
            )
            println("QRHASSSSSS: "+ qrCode.qr)
            if ((qrCode.qr == null) || (qrCode.qrhash == null) || (qrCode.ShortUrlhash == null)) throw QRCodeUriNotFoundException(qrCode.qrhash, " is not reachable")
            qrCodeRepository.save(qrCode)
        }else{
            println("EXISTE")
        }
        return qrCode
        //println("LEE FICHERO: " +qrCode.qr)

    }
}
