package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 *
 *
 * **Note**: This is an example of functionality.
 */

interface QRGeneratorUseCase {
    fun create(data: String,qrdata: String): QRCode
}

/**
 * Implementation of [QRGeneratorUseCase].
 */
class QRGeneratorUseCaseImpl(
    private val qrCodeRepository: QRCodeRepositoryService,
    private val qrService: QRService,
    private val hashService: HashService
) : QRGeneratorUseCase {
    override fun create(data: String, qrdata: String): QRCode {
       
        var qrc = qrService.qr("http://localhost:8080/tiny-" + data)
        var qrCode = qrCodeRepository.findByKey(qrdata)

        if(qrCode == null){
            println("NO EXISTE")
            qrCode = QRCode(
                //qrhash = hashService.hasUrl("http://localhost/" + data),
                qrhash = qrdata,
                ShortUrlhash = data,
                qr = qrService.qrbytes(qrc)
            )
            println("QRHASSSSSS: "+ qrCode.qr)
            if ((qrCode.qr == null) || (qrCode.qrhash == null) || (qrCode.ShortUrlhash == null)) throw QRCodeUriNotFoundException(qrCode.qrhash, " uri de destino no validada todavía")
            qrCodeRepository.save(qrCode)
        }else{
            println("EXISTE")
        }
        return qrCode
        //println("LEE FICHERO: " +qrCode.qr)

    }
}
