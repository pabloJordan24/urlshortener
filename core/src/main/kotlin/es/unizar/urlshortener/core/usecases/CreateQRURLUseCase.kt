package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 *
 *
 * **Note**: This is an example of functionality.
 */

interface CreateQRURLUseCase {
    fun create(data: String,): String
}

/**
 * Implementation of [QRGeneratorUseCase].
 */
class CreateQRURLUseCaseImpl(
    private val hashService: HashService
) : CreateQRURLUseCase {
    override fun create(data: String): String {
       //Genero el hash y ya el thread comprueba en su caso de uso si existe o si no
        return hashService.hasUrl("http://localhost/" + data)
    }
}
