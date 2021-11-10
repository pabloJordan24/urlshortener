package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.io.BufferedReader



/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateCsvShortUrlUseCase {
    fun create(reader: BufferedReader, reqip: String): MutableMap<String,String>
}

class CreateCsvShortUrlUseCaseImpl(
    private val createShortUrlUseCase: CreateShortUrlUseCase,
    private val redirectUseCase: RedirectUseCase
): CreateCsvShortUrlUseCase {
    override fun create(reader: BufferedReader, reqip: String): MutableMap<String,String> {
        val iterator = reader.lineSequence().iterator()
        val original_shortened_map = mutableMapOf<String, String>()
        //leemos como buenos perros
        while(iterator.hasNext()) {
            val line = iterator.next()
                        
            //ver si se puede shortenear la URL
            try {
                val su = createShortUrlUseCase.create(url = line,
                    data = ShortUrlProperties(
                        ip = reqip,
                        sponsor = null
                    )
                )
                original_shortened_map[line] = su.hash
            }
            //si el create no devuelve una shortUrl, tirará exception (una de estas dos).
            catch(e: InvalidUrlException) {
                //println(e.message)
                original_shortened_map[line] = e.message.toString()
            }
            catch(e: NotReachableException) {
                //println(e.message)
                original_shortened_map[line] = e.message.toString()
            }

        }
        reader.close()

        return original_shortened_map
    }
}