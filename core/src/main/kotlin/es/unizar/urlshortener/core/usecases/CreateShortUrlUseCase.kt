package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (validatorService.isValid(url)) {
            val id: String = hashService.hasUrl(url)
            val su = ShortUrl(
                hash = id,
                redirection = Redirection(target = url),
                properties = ShortUrlProperties(
                    safe = data.safe,
                    ip = data.ip,
                    sponsor = data.sponsor
                )
            )
            println(shortUrlRepository.showAll())
            //if shortUrl already exists return it, if not, save it.
            val shortUrl = shortUrlRepository.findByKey(id)     
            if (shortUrl!=null) {
                println(url + " no almacenada en BD (ya estaba).")
                shortUrlRepository.findByKey(id) as ShortUrl
            }
            else {
                println(url + " almacenada en BD.")
                shortUrlRepository.save(su)
            }

        } else {
            throw InvalidUrlException(url)
        }
}


//¿cuál es el problema aquí?
//Que tal y como está hecha la bd, una misma shortUrl se puede almacenar
//varias veces (id no es único). Entonces, cuando pidan datos de una shortUrl,
//¿qué le damos (porque puede haber varias rows iguales)?

//Lo que se puede hacer es dejarlo como está, ya que si dos personas quieren acortar
//la misma URL, deben poder hacerlo. Lo que debemos hacer nosotros es mostrar la info
//de la URl acortada para el usuario x (el que hace la petición). Se supone que el 
//que pide info de la url acortada es el que la acortó en su momento.

//o si no, sacamos el número de veces que se ha acortado esa URL (contando ocurrencias en bd),
//los clicks a esa URL tb se podrían sacar, ...