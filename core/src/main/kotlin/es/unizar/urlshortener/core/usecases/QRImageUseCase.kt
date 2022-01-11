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

        if (qrcode==null) throw QRCodeUriNotFoundException(id, "QR not created yet")
        
        else {
            //search for shorturl uri in DB
            val shortUrl = shortUrlRepository.findByKey(qrcode.ShortUrlhash)
            //does not exist
            if (shortUrl!=null && shortUrl.properties.safe=="not validated") {
                throw QRCodeUriNotFoundException(qrcode.ShortUrlhash,  "URI not validated yet")
            }

            //if it exists and reacheable, return bytes sec
            else {
                if (shortUrl!=null && shortUrl.properties.safe!="safe") {
                    throw QRCodeUriNotFoundException(qrcode.ShortUrlhash,  "URI not reachable")
                }   
                val imgbytes = qrcode.qr

                return QRCode(
                    qrhash=  id,
                    ShortUrlhash= qrcode.ShortUrlhash,
                    qr=  imgbytes
                )
            }
        }
        
    }
}