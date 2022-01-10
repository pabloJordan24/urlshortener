package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.ReachableServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.*
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.*
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.*
import org.springframework.scheduling.concurrent.*

import java.util.concurrent.*

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
@EnableAsync
@EnableScheduling
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val qrCodeEntityRepository: QRCodeEntityRepository
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun reachableService() = ReachableServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() = CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), reachableService(), hashService(), alcanzableUseCase())

    @Bean
    fun infoShortUrlUseCase() = InfoShortUrlUseCaseImpl(shortUrlRepositoryService(), clickRepositoryService())

    @Bean
    fun createCsvShortUrlUseCase() = CreateCsvShortUrlUseCaseImpl(createShortUrlUseCase(), redirectUseCase())

    @Bean
    fun alcanzableUseCase() = AlcanzableUseCaseImpl(shortUrlRepositoryService(), reachableService())

    @Bean
    fun qrCodeRepositoryService() = QRCodeRepositoryServiceImpl(qrCodeEntityRepository)

    @Bean
    fun qrImageUseCase() = QRImageUseCaseImpl(shortUrlRepositoryService(), qrCodeRepositoryService())

    @Bean
    fun qrService() = QRServiceImpl()

    @Bean
    fun qrGeneratorUseCase() = QRGeneratorUseCaseImpl(qrCodeRepositoryService(), qrService(),hashService())

    @Bean
    fun createQRURLUseCase() = CreateQRURLUseCaseImpl(hashService(), alcanzableUseCase(), qrGeneratorUseCase())

    /*@Bean
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor() 
        executor.corePoolSize = 2 
        executor.maxPoolSize = 2 
        executor.setQueueCapacity(500) 
        executor.setThreadNamePrefix("Task-") 
        executor.initialize()
        return executor
    }*/
}