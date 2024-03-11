package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.AGENT_NAME
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.META_SERVICE_URL
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.SPIEGEL_TEST_FEED_URL
import com.mesmoray.lektora.e2etests.metaservice.CategoryCRUDSimulation.Companion.createCategory
import com.mesmoray.lektora.e2etests.metaservice.CategoryCRUDSimulation.Companion.deleteCategory
import com.mesmoray.lektora.e2etests.metaservice.CountryCRUDSimulation.Companion.createCountry
import com.mesmoray.lektora.e2etests.metaservice.CountryCRUDSimulation.Companion.deleteCountry
import com.mesmoray.lektora.e2etests.metaservice.LanguageCRUDSimulation.Companion.createLanguage
import com.mesmoray.lektora.e2etests.metaservice.LanguageCRUDSimulation.Companion.deleteLanguage
import com.mesmoray.lektora.e2etests.metaservice.PublisherCRUDSimulation.Companion.createPublisher
import com.mesmoray.lektora.e2etests.metaservice.PublisherCRUDSimulation.Companion.deletePublisher
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomCountryCode
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomLanguageCode
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomString
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import   io.gatling.javaapi.http.HttpRequestActionBuilder

class FeedCRUDSimulation : Simulation() {
    companion object {
        private val feedName = randomString(5)
        private const val FEED_FORMAT = "RSS"
        private const val feedUrl = SPIEGEL_TEST_FEED_URL
    }

    private val httpProtocol =
        http.baseUrl(META_SERVICE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader(
                AGENT_NAME,
            )

    private val healthCheck =
        exec(
            http("Health Check")
                .get("/health")
                .check(status().shouldBe(200))
                .check(bodyString().`is`("Application is healthy")),
        )

    private val createFeed =
        exec(
            http("Create Feed")
                .post("/feeds")
                .body(
                    StringBody(
                        """
                        {
                            "name": "$feedName",
                            "url": "$feedUrl",
                            "countryCode": "#{countryCode}", 
                            "languageCode":"#{languageCode}",
                            "categoryId": "#{categoryId}",
                            "publisherId": "#{publisherId}",
                            "format": "$FEED_FORMAT"
                        }
                        """.trimIndent(),
                    ),
                )
                .check(status().shouldBe(201))
                .checkFeed()
                .check(jsonPath("$.id").saveAs("feedId")),
        )

    private fun HttpRequestActionBuilder.checkFeed() =
        check(
            jsonPath("$.name").shouldBe(feedName),
            jsonPath("$.url").shouldBe(feedUrl),
            jsonPath("$.countryCode").shouldBe {session -> session.get("countryCode")},
            jsonPath("$.languageCode").shouldBe{session -> session.get("languageCode")},
            jsonPath("$.categoryId").shouldBe{session -> session.get("categoryId")},
            jsonPath("$.publisherId").shouldBe{session -> session.get("publisherId")},
        )

    private val readFeed =
        exec(
            http("Read Feed")
                .get("/feeds/#{feedId}")
                .check(status().shouldBe(200))
                .checkFeed()
        )

    private val updateFeed =
        exec(
            http("Update Feed")
                .put("/feeds/#{feedId}")
                .body(
                    StringBody(
                        """
                        {
                            "id": "#{feedId}",
                            "name": "$feedName",
                                                        "url": "$feedUrl",
                                                        "countryCode": "#{countryCode}", 
                                                        "languageCode":"#{languageCode}",
                                                        "categoryId": "#{categoryId}",
                                                        "publisherId": "#{publisherId}",
                                                        "format": "$FEED_FORMAT"                       
                        }
                        """.trimIndent(),
                    ),
                )
                .check(status().shouldBe(200)),
        )

    private val deleteFeed =
        exec(
            http("Delete Feed")
                .delete("/feeds/#{feedId}")
                .check(status().shouldBe(204)),
        )

    private val getCountryFeeds =
        exec(
            http("Get Country Feeds")
                .get("/feeds/country/#{countryCode}")
                .check(status().shouldBe(200)),
        )

    private val getLanguageFeeds =
        exec(
            http("Get Language Feeds")
                .get("/feeds/language/#{languageCode}")
                .check(status().shouldBe(200)),
        )

    private val getPublisherFeeds =
        exec(
            http("Get Publisher Feeds")
                .get("/feeds/publisher/#{publisherId}")
                .check(status().shouldBe(200)),
        )

    private val publishers =
        CoreDsl.scenario("Feeds")
            .exec(
                healthCheck
                    .exec(createCountry)
                    .exec(createLanguage)
                    .exec(createCategory)
                    .exec(createPublisher)
                    .exec(createFeed)
                    .exec(readFeed)
                    .exec(updateFeed)
                    .exec(getCountryFeeds)
                    .exec(getLanguageFeeds)
                    .exec(getPublisherFeeds)
                    .exec(deleteFeed)
                    .exec(deleteCategory)
                    .exec(deleteCountry)
                    .exec(deleteLanguage)
                    .exec(deletePublisher)
            )

    init {
        setUp(
            publishers.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
