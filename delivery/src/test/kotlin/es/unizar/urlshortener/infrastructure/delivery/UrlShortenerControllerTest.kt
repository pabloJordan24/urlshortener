package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.InfoShortUrlUseCase
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.OffsetDateTime

@WebMvcTest
@ContextConfiguration(classes = [
    UrlShortenerControllerImpl::class,
    RestResponseEntityExceptionHandler::class])
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var infoShortUrlUseCase: InfoShortUrlUseCase

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        given(redirectUseCase.redirectTo("key")).willReturn(Redirection("http://example.com/"))

        mockMvc.perform(get("/tiny-{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key"))
            .willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/tiny-{id}", "key"))
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(createShortUrlUseCase.create(
            url = "http://example.com/",
            data = ShortUrlProperties(ip = "127.0.0.1")
        )).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(post("/api/link")
            .param("url", "http://example.com/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/tiny-f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/tiny-f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(createShortUrlUseCase.create(
            url = "ftp://example.com/",
            data = ShortUrlProperties(ip = "127.0.0.1")
        )).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(post("/api/link")
            .param("url", "ftp://example.com/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `creates returns bad request if URI not reachable`() {
        given(createShortUrlUseCase.create(
            url = "https://www.hola.com/kalskladkale",
            data = ShortUrlProperties(ip = "127.0.0.1")
        )).willAnswer { throw NotReachableException("https://www.hola.com/kalskladkale"," is not reachable") }

        mockMvc.perform(post("/api/link")
            .param("url", "https://www.hola.com/kalskladkale")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.error").value("[https://www.hola.com/kalskladkale] is not reachable"))
    }

    @Test
    fun `creates returns ok if URI is reachable`() {
        given(createShortUrlUseCase.create(
            url = "https://www.hola.com/",
            data = ShortUrlProperties(ip = "127.0.0.1")
        )).willReturn(ShortUrl("b8de6817", Redirection("https://www.hola.com/")))

        mockMvc.perform(post("/api/link")
            .param("url", "https://www.hola.com/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/tiny-b8de6817"))
            .andExpect(jsonPath("$.url").value("http://localhost/tiny-b8de6817"))
    }

    @Test
    fun `getURL info returns information of existing URI`() {
        //creamos la URL por si no estuviese de antes
        given(createShortUrlUseCase.create(
            url = "https://google.com/",
            data = ShortUrlProperties(ip = "127.0.0.1")
        )).willReturn(ShortUrl("bf8e423d", Redirection("https://google.com/")))

        //ejecutamos el método de info
        given(infoShortUrlUseCase.info("bf8e423d")).willReturn(ShortUrlInfo(0,checkTime(),"https://google.com/"))

        //petición GET a /info/{id} para ver si funciona
        mockMvc.perform(get("/info/bf8e423d"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numClicks").value(0))
        .andExpect(jsonPath("$.creationDate").value(checkTime()))
        .andExpect(jsonPath("$.uriDestino").value("https://google.com/"))
    }

    @Test
    fun `getURL info returns not found if we pass a non existing URI`() {
        //ejecutamos el método de info con URI no existente
        given(infoShortUrlUseCase.info("aaaaaaaa")).willAnswer { throw RedirectionNotFound("aaaaaaaa") }

        //esto falla ni idea porqué
        mockMvc.perform(get("/info/{id}","aaaaaaaa"))
        .andExpect(status().isNotFound)
        .andExpect(jsonPath("$.statusCode").value(404))
    }

    private fun checkTime(): String {
        val date = OffsetDateTime.now()
        return (
            date.getDayOfMonth().toString()+"-"+
            date.getMonth().toString()+"-"+
            date.getYear().toString()
        )
    }
}