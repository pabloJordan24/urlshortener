package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Executor
import mu.KotlinLogging
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 *
 *
 * **Note**: This is an example of functionality.
 */

interface CreateQRURLUseCase {
    /* Thread Pool: https://developer.android.com/reference/kotlin/java/util/concurrent/Executors */
    val executor: Executor

    /* 
     * Task queue (in this case, it generates QR from a given URI).
     */
    var colaGeneracionQR: LinkedBlockingQueue<Runnable>

    fun create(data: String,): String
}

/***** QR *****/
class TareaCrearQR (
    val urlhash: String,
    val qrhash: String,
    val qrGeneratorUseCase: QRGeneratorUseCase,
) : Runnable {
    override fun run() {
        val logger = KotlinLogging.logger {}
        logger.info { "[QR] : Tarea crear QR empieza" }
        //meter aquí en vez del sleep el servicio de comprobar la alcanzabilidad
        Thread.sleep(5_000)
        qrGeneratorUseCase.create(urlhash,qrhash)
        logger.info { "[QR] : Tarea crear QR acabada" }
    }
}

/***** Clase que gestiona las tareas de la cola *****/
class ManageQueue2 (
    val executor: Executor,
    var cola: LinkedBlockingQueue<Runnable>
) : Runnable {
    override fun run() {
        while(true) {
            val logger = KotlinLogging.logger {}
            val t = cola.take()
            executor.execute(t)
            logger.info { "[QUEUE] : Tarea recogida" }
        }
    }
}

/**
 * Implementation of [QRGeneratorUseCase].
 */
class CreateQRURLUseCaseImpl(
    private val hashService: HashService,
    private val alcanzableUseCase: AlcanzableUseCase,
    private val qrGeneratorUseCase: QRGeneratorUseCase
) : CreateQRURLUseCase {

    //task exec.
    override val executor = taskExecutor(2,2)

    //our queue
    override var colaGeneracionQR = LinkedBlockingQueue<Runnable>()

    init {
        //lanzamos un thread que inicialice nuestra cola y al thread pool que
        //lee de ella.
        var lanzador = taskExecutor(1,1)
        lanzador.execute(ManageQueue2(executor,colaGeneracionQR))
    }
    override fun create(data: String): String {
        //hasheo qr
        val qrHash = hashService.hasUrl("http://localhost/" + data)

        //Tarea de crear qr + tarea de mirar alcanzabilidad
        val tareaQR = TareaCrearQR(data, qrHash, qrGeneratorUseCase)
        colaGeneracionQR.put(tareaQR)
        
        //Genero el hash y ya el thread comprueba en su caso de uso si existe o si no
        return qrHash
    }
}
