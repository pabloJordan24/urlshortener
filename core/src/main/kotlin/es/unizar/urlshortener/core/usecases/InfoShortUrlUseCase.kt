package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date
import java.text.SimpleDateFormat
import org.springframework.scheduling.annotation.Scheduled
import java.time.OffsetDateTime

/**
 * Given an url returns information about it.
 *
 * **Note**: This is an example of functionality.
 */
interface InfoShortUrlUseCase {    
    var stats: HashMap<String,ShortUrlInfo>
    fun info(id: String): ShortUrlInfo
    fun basicScheduler(): Unit
    fun showStats(id: String): ShortUrlInfo
}

/**
 * Implementation of [InfoShortUrlUseCase].
 */
class InfoShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val clickRepository: ClickRepositoryService
) : InfoShortUrlUseCase {
    override var stats = HashMap<String,ShortUrlInfo>()
    
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
            var now = OffsetDateTime.now()
            var sevenDaysAgo = now.minusDays(7L)
            var usuariosURI_haceSieteDias = clickRepository.fetchIPClient(id,sevenDaysAgo)

            return ShortUrlInfo(
                clicks=numClicks,
                created=created.getDayOfMonth().toString()+"-"+
                created.getMonth().toString()+"-"+
                created.getYear().toString(),
                uri=target,
                users=usuariosURI_haceSieteDias
            )
        }
    }

    // función que se ejecuta cada cierto periodo de tiempo y recopila la info
    // de las short urls que hay almacenadas en BD.
    @Scheduled(fixedRate = 10000L)
    override fun basicScheduler(): Unit {
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        
        println("[+] -- Scheduler basico: " + currentDate)

        var listsu = shortUrlRepository.showAll()
        for (su in listsu) {
            val id = su.hash
            //search for number of clicks of given URL
            val numClicks = clickRepository.countByHash(id)
            val created = su.created
            val target = su.redirection.target
            // *********
            var now = OffsetDateTime.now()
            var sevenDaysAgo = now.minusDays(7L)
            var usuariosURI_haceSieteDias = clickRepository.fetchIPClient(id,sevenDaysAgo)

            val sui =  ShortUrlInfo(
                clicks=numClicks,
                created=created.getDayOfMonth().toString()+"-"+
                created.getMonth().toString()+"-"+
                created.getYear().toString(),
                uri=target,
                users=usuariosURI_haceSieteDias
            )
            
            
            println("Clicks de usuarios distintos a la shortUrl "+id+": "+usuariosURI_haceSieteDias)
            
            stats.put(id,sui)
        }
        println("[-] -- Scheduler basico: tamanyo del hashMap -> " + stats.size)
    }

    override fun showStats(id: String): ShortUrlInfo {
        val sui = stats.get(id)
        if (sui!=null) return sui
        else throw RedirectionNotFound(id)
    }
}

// *********
// si quiero clicks en los ultimos 7 dias -> calculo el offsetdatetime de hace 
// siete dias y llamo a la funcion fetchIpCLient. Le paso ese offset time y
// todos los records de la bd cuyo created sea mayor/igual que el que le paso, 
// son clicks hechos por users en los ultimos 7 dias.