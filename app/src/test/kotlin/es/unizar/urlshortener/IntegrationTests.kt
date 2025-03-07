package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import es.unizar.urlshortener.infrastructure.delivery.ShortUrlInfoData
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.OffsetDateTime
import es.unizar.urlshortener.infrastructure.delivery.*


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HttpRequestTest {
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
        val target = shortUrl("http://example.com/").headers.location
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
        val response = shortUrl("http://example.com/")

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
    fun `test info of existing shortened URL`() {
        val response = shortUrl("https://google.com/")

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/tiny-bf8e423d"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/tiny-bf8e423d"))

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)

        //hacemos un redirect a esa URL para que haya 1 click
        restTemplate.getForEntity(response.headers.location, String::class.java)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(1)    

        //hacemos un get a /info/{id} para ver si cuenta con 1 click y ya de paso miramos el resto de stats
        val response2 = restTemplate.getForEntity("/info/bf8e423d", ShortUrlInfoData::class.java)
        assertThat(response2.body?.numClicks).isEqualTo(1)
        assertThat(response2.body?.creationDate).isEqualTo(checkTime())
        assertThat(response2.body?.uriDestino).isEqualTo(URI.create("https://google.com/"))

        //clicamos dos veces más en la tiny URL (tiny-bf8e423d)
        restTemplate.getForEntity(response.headers.location, String::class.java)
        restTemplate.getForEntity(response.headers.location, String::class.java)

        //hacemos el get /info/{id} y comprobamos esos 3 clicks
        val response4 = restTemplate.getForEntity("/info/bf8e423d", ShortUrlInfoData::class.java)
        assertThat(response4.body?.numClicks).isEqualTo(3)
        assertThat(response4.body?.creationDate).isEqualTo(checkTime())
        assertThat(response4.body?.uriDestino).isEqualTo(URI.create("https://google.com/"))
    }

    @Test
    fun `test info of non existing shortened URL`() {
        
        //hacemos un get a /info/{id} donde el id NO EXISTE.
        val response = restTemplate.getForEntity("/info/aaaaaaaa", ErrorMessage::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(0)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "click")).isEqualTo(0)
    }

    @Test
    fun `Create shortened URL starting from a not reachable URL`() {
        //hacemos un POST a /api/link con una URL que no es alcanzable (no devuelve 200)
        val response = shortUrl("https://www.hola.com/kalskladkale")
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `Create shortened URL starting from a reachable URL`() {     
        //hacemos un POST a /api/link con una URL que no es alcanzable (no devuelve 200)
        val response = shortUrl("https://www.hola.com/")
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.headers.location).isEqualTo(URI.create("http://localhost:$port/tiny-b8de6817"))
        assertThat(response.body?.url).isEqualTo(URI.create("http://localhost:$port/tiny-b8de6817"))
    }


    private fun shortUrl(url: String): ResponseEntity<ShortUrlDataOut> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = url

        return restTemplate.postForEntity(
            "http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java
        )
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