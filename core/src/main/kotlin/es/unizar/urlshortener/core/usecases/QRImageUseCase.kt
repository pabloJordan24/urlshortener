package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date

/**
 * Given an url returns QRCode.
 *
 * **Note**: This is an example of functionality.
 */
interface QRImageUseCase {
    fun image(id: String): QRCode
}

/**
 * Implementation of [QRImageUseCaseImpl].
 */
class QRImageUseCaseImpl (
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val qrCodeRepository: QRCodeRepositoryService,
) : QRImageUseCase {
    override fun image(id: String): QRCode {

        //id is qrhash

        println("HASHH1: "+id)
        //search for qrcode in DB
        val qrcode = qrCodeRepository.findByKey(id)

       if (qrcode==null)throw QRCodeUriNotFoundException(id, " is not reachable")
        else{
            println("hola")
        }
        //search for shorturl uri in DB
        val shortUrl = shortUrlRepository.findByKey(qrcode.ShortUrlhash)
        //does not exist

        if (shortUrl==null) throw QRCodeUriNotFoundException(qrcode.ShortUrlhash,  " uri de destino no validada todavía")

        //if it exists, return bytes sec
        else {

            val imgbytes = qrcode.qr

            return QRCode(
                qrhash=  id,
                ShortUrlhash= qrcode.ShortUrlhash,
                qr=  imgbytes
            )
        }
    }
}