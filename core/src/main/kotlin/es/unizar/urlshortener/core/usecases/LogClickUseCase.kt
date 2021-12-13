package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService
import java.util.Date

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = ClickProperties(
                ip = data.ip
                //colocar aquí el timestamp del click
            )
        )
        println(clickRepository.showAll())
        clickRepository.save(cl)
    }
}

//cuando haya que sacar los últimos diez usuarios del click, hacemos:
//group by timestamp (order by timestamp DESC) y hago limit 10 pa coger 
//los 10 primeros. Proyecto la ip de esas tuplas pa sacar los 10 usuarios.

//Puedo sacar también el browser del cual se hizo la petición de creación.

//Tenemos el número de clicks totales de la URI, pero podemos sacar el número de 
//clicks por día en la última semana, por ejemplo.