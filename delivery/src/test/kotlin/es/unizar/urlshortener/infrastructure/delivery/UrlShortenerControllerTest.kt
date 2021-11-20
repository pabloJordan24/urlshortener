package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
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
    private lateinit var qrGeneratorUseCase: QRGeneratorUseCase

    @MockBean
    private lateinit var qrImageUseCase: QRImageUseCase
    @MockBean
    private lateinit var qrService: QRService

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
    fun `create returns a qr for de shorurl`() {

        given(createShortUrlUseCase.create(
            url = "http://google.com",
            data = ShortUrlProperties(ip = "127.0.0.1"),
        )).willReturn(ShortUrl("58f3ae21", Redirection("http://google.com")))

        given(qrGeneratorUseCase.create(
            data = "58f3ae21"
        )).willAnswer { QRCode("0a9143c7","58f3ae21", qrService.qrbytes(qrService.qr("http://localhost/tiny-58f3ae21")))}
        mockMvc.perform(post("/api/link")
            .param("url", "http://google.com")

            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/tiny-58f3ae21"))
            .andExpect(jsonPath("$.qr").value("http://localhost:8080/qrcode-0a9143c7"))

    }





}