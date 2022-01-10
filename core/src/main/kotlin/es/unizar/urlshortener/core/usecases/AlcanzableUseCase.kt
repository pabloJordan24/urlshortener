package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection

/**
 * Given an [url], this method proves wether it is reachable or not.
 */
interface AlcanzableUseCase {
    fun esAlcanzable(key: String): Unit
}

/**
 * Implementation of [AlcanzableUseCase].
 */
class AlcanzableUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val reachableService: ReachableService
) : AlcanzableUseCase {
    override fun esAlcanzable(key: String): Unit {
        //get shortenedURL
        var su = shortUrlRepository.findByKey(key)
        if (su!=null) {
            val esAlcanzable = reachableService.isReachable(su.redirection.target)
            if (esAlcanzable) {
                //update record in db. Now it is reachable.
                su.properties.safe="safe"
                shortUrlRepository.save(su)
            }
            else {
                su.properties.safe="not reachable"
                shortUrlRepository.save(su)
                throw NotReachableException(su.redirection.target," is not reachable")
            }
        }
        else throw RedirectionNotFound(key)
    }
}
