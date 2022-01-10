package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Redirection
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.NotReachableException
import es.unizar.urlshortener.core.ShortUrlRepositoryService

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService
) : RedirectUseCase {
    /*override fun redirectTo(key: String) = shortUrlRepository
        .findByKey(key)
        ?.redirection
        ?: throw RedirectionNotFound(key)*/

    override fun redirectTo(key: String) : Redirection {
        val su = shortUrlRepository.findByKey(key)
        //su does not exist
        if (su==null) throw RedirectionNotFound(key)
        //su has not been validated yet
        if (su.properties.safe=="not validated") throw NotReachableException(key, " not validated yet")
        else if (su.properties.safe=="not reachable") throw NotReachableException(key, " not reachable")

        return su.redirection

    }
}
