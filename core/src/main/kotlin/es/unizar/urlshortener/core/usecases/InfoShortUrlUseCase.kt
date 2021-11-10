package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date

/**
 * Given an url returns information about it.
 *
 * **Note**: This is an example of functionality.
 */
interface InfoShortUrlUseCase {
    fun info(id: String): ShortUrlInfo
}

/**
 * Implementation of [InfoShortUrlUseCase].
 */
class InfoShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val clickRepository: ClickRepositoryService
) : InfoShortUrlUseCase {
    override fun info(id: String): ShortUrlInfo {
        println("INFO USE CASE: " + id)

        //search for tinyURL in DB
        val shortUrl = shortUrlRepository.findByKey(id)     
        
        //does not exist
        if (shortUrl==null) throw RedirectionNotFound(id)

        //if it exists, search for information
        else {
            //search for number of clicks of given URL
            val numClicks = clickRepository.countByHash(id)
            val created = shortUrl.created
            val target = shortUrl.redirection.target

            return ShortUrlInfo(
                clicks=numClicks,
                created=created.getDayOfMonth().toString()+"-"+
                created.getMonth().toString()+"-"+
                created.getYear().toString(),
                uri=target
            )
        }
    }
}
