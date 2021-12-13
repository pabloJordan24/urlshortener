package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import mu.KotlinLogging
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.Date
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {

    /* Thread Pool: https://developer.android.com/reference/kotlin/java/util/concurrent/Executors */
    val executor: Executor

    /* Task queue (in this case, it only proves if shortUrl is reachable).
     * It is defined as String type because we only need the URL´s key.
     */
    var colaAlcanzabilidad: LinkedBlockingQueue<Runnable>

    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/*
 * Function that returns an executor (thread pool).
 */
fun taskExecutor(mycorePoolSize:Int, mymaxPoolSize:Int): Executor {
    val executor = ThreadPoolTaskExecutor()
    executor.corePoolSize = mycorePoolSize
    executor.maxPoolSize = mymaxPoolSize
    executor.setQueueCapacity(500)
    executor.setThreadNamePrefix("Task-")
    executor.initialize()
    return executor
}

//meter la concurrentqueue con el thread que lea de la cola

class TareaComprobarUrlAlcanzable (
    val key: String,
    val alcanzableUseCase: AlcanzableUseCase
) : Runnable {
    override fun run() {
        val logger = KotlinLogging.logger {}
        //meter aquí en vez del sleep el servicio de comprobar la alcanzabilidad
        Thread.sleep(5_000)
        alcanzableUseCase.esAlcanzable(key)
        logger.info { "Tarea acabada" }
    }
}

class ManageQueue (
    val executor: Executor,
    var cola: LinkedBlockingQueue<Runnable>
) : Runnable {
    override fun run() {
        while(true) {
            val t = cola.take()
            executor.execute(t)
        }
    }
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService,
    private val alcanzableUseCase: AlcanzableUseCase
) : CreateShortUrlUseCase {

    override val executor = taskExecutor(2,2)

    override var colaAlcanzabilidad = LinkedBlockingQueue<Runnable>()

    init {
        //lanzamos un thread que inicialice nuestra cola y al thread pool que
        //lee de ella.
        var lanzador = taskExecutor(1,1)
        lanzador.execute(ManageQueue(executor,colaAlcanzabilidad))
    }

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (validatorService.isValid(url)) {
            //if (reachableService.isReachable(url)) {
                val id: String = hashService.hasUrl(url)
                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        //safe = data.safe,
                        //escalabilidad: devolvemos al user la url sin comprobar AÚN
                        //si es alcanzable o no lo es.
                        safe = false,
                        ip = data.ip,
                        sponsor = data.sponsor
                    )
                )
                println(shortUrlRepository.showAll())
                //if shortUrl already exists return it, if not, save it.
                val shortUrl = shortUrlRepository.findByKey(id)

                //lanzo tarea de comprobar la alcanzabilidad de la url devuelta
                val tarea = TareaComprobarUrlAlcanzable(id,alcanzableUseCase)
                //executor.execute(tarea)
                //en el nivel 15 -> encolamos la tarea (con el hash a comprobar).
                colaAlcanzabilidad.put(tarea)

                if (shortUrl!=null) {
                    println(url + " no almacenada en BD (ya estaba).")
                    shortUrlRepository.findByKey(id) as ShortUrl
                }
                else {
                    println(url + " almacenada en BD.")
                    shortUrlRepository.save(su)
                }
            /*}
            else {
                println("NotReachableException")
                throw NotReachableException(url," is not reachable")
            }*/
        } else {
            println("InvalidUrlException")
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