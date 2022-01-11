package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Executor
import mu.KotlinLogging
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor


/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    /* Thread Pool: https://developer.android.com/reference/kotlin/java/util/concurrent/Executors */
    val executor: Executor

    /* 
     * Task queue (in this case, it only proves if shortUrl is reachable)..
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

/***** Alcanzabilidad *****/
class TareaComprobarUrlAlcanzable (
    val key: String, 
    val alcanzableUseCase: AlcanzableUseCase
    ) : Runnable {
    override fun run() {
        try {
            val logger = KotlinLogging.logger {}
            logger.info { "[AL] : Tarea alcanzabilidad empieza" }
            //meter aquí en vez del sleep el servicio de comprobar la alcanzabilidad
            Thread.sleep(7_000)
            alcanzableUseCase.esAlcanzable(key)
            logger.info { "[AL] : Tarea alcanzabilidad acabada" }
        } catch(ex : Exception) {
            val logger = KotlinLogging.logger {}
            logger.info { "[AL] : No es alcanzable la URI" }
            //throw ex
        }
    }
}

/***** Clase que gestiona las tareas de la cola *****/
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
    private val reachableService: ReachableService,
    private val hashService: HashService,
    private val alcanzableUseCase: AlcanzableUseCase
) : CreateShortUrlUseCase {
    
    //task exec.
    override val executor = taskExecutor(2,2)

    //our queue
    override var colaAlcanzabilidad = LinkedBlockingQueue<Runnable>()

    init {
        //lanzamos un thread que inicialice nuestra cola y al thread pool que
        //lee de ella.
        var lanzador = taskExecutor(1,1)
        lanzador.execute(ManageQueue(executor,colaAlcanzabilidad))
    }

    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (validatorService.isValid(url)) {
            val id: String = hashService.hasUrl(url)
            val su = ShortUrl(
                hash = id,
                redirection = Redirection(target = url),
                properties = ShortUrlProperties(
                    //escalabilidad: devolvemos al user la url sin comprobar AÚN 
                    //si es alcanzable o no lo es. 
                    safe = "not validated",
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
                //lanzo tarea de comprobar la alcanzabilidad de la url devuelta
                val tarea = TareaComprobarUrlAlcanzable(id,alcanzableUseCase)
                //en el nivel 15 -> encolamos la tarea (con el hash a comprobar).
                colaAlcanzabilidad.put(tarea)

                shortUrlRepository.save(su)
            }
        } else {
            println("InvalidUrlException")
            throw InvalidUrlException(url)
        }
}