package es.unizar.urlshortener


import es.unizar.urlshortener.core.QRCode
import es.unizar.urlshortener.core.QRCodeUriNotFoundException
import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
    data class ShortUrlDataIn(
        val url: String,
        //val qr: Boolean

    )
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @Test
    fun `main page works`() {
        val response = restTemplate.getForEntity("http://localhost:$port/", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("A front-end example page for the project")
    }

    @Test
    fun `redirectTo returns a redirect when the key exists`() {
        val target = shortUrl("http://example.com/","false").headers.location
        require(target != null)
        val response = restTemplate.getForEntity(target, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers.location).isEqualTo(URI.create("http://example.com/"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        val response = restTemplate.getForEntity("http://localhost:$port/f684a3c4", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        val response = shortUrl("http://example.com/","false")
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/tiny-f684a3c4"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `creates returns bad request if it can't compute a hash`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "ftp://example.com/"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `create QRCode returns an invalid QRCode`() {
        val contenidoBD = JdbcTestUtils.countRowsInTable(jdbcTemplate, "qrcode")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "hftp://example.com/"
        data["qr"] = "true"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "qrcode")).isEqualTo(contenidoBD)

    }

    @Test
    fun `create QRCode returns valid QRCode`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://google.com/"
        data["qr"] = "true"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)

        assertThat(response.statusCode).isNotEqualTo(HttpStatus.BAD_REQUEST)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "qrcode")).isNotEqualTo(0)
    }

    @Test
    fun `create QRCode returns an invalid QRCode when invalid input data`() {
        val response = qrCode("htts://www.hola.com/kalskladkale","true")
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `image QRCode returns an invalid QRCode when not exists qr hash`(){
        //assertThrows(QRCodeUriNotFoundException::class.java) {
            val response = restTemplate.getForEntity("/qrcode-hola", ByteArray::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        //}

    }


    @Test
    fun `image QRCode returns valid QRCode found in database`() {
        val post =  qrCode("http://google.com/", "true")
        val hashQR = post.body?.qr
        val qrhash = hashQR.toString().split("qrcode-")
        assertThat(post.statusCode).isNotEqualTo(HttpStatus.BAD_REQUEST)
        val response = restTemplate.getForEntity("/qrcode-"+qrhash[1], ByteArray::class.java)
        println(response.statusCode)
        assertThat(response.statusCode).isNotEqualTo(HttpStatus.BAD_REQUEST)
    }


    private fun shortUrl(url: String, checked: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["qr"] = checked

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
    }

    private fun qrCode(url: String, check: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url
        data["qr"] = check

        return restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)
    }
}