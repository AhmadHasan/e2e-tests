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

class FeedCRUDSimulation : Simulation() {
    companion object {
        private val publisherName = randomString(5)
        private val languageCode = randomLanguageCode()
        private val countryCode = randomCountryCode()
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
                            "url": "$feedUrl"
                            "countryCode": "$countryCode", 
                            "mainLanguageCode":"$languageCode",
                            "categoryId": "#{categoryId}",
                            "publisherId": "#{publisherId}",
                            "format": "$FEED_FORMAT"
                        }
                        """.trimIndent(),
                    ),
                )
                .check(status().shouldBe(201))
                .check(jsonPath("$.name").shouldBe(publisherName))
                .check(jsonPath("$.url").shouldBe(feedUrl))
                .check(jsonPath("$.countryCode").shouldBe(countryCode))
                .check(jsonPath("$.languageCode").shouldBe(languageCode))
                .check(jsonPath("$.id").saveAs("feedId")),
        )

    private val readFeed =
        exec(
            http("Read Feed")
                .get("/feeds/#{feedId}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(feedName))
                .check(jsonPath("$.url").shouldBe(feedUrl))
                .check(jsonPath("$.countryCode").shouldBe(countryCode))
                .check(jsonPath("$.languageCode").shouldBe(languageCode)),
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
                            "url": "$feedUrl"
                            "countryCode": "$countryCode", 
                            "mainLanguageCode":"$languageCode",
                            "categoryId": "#{categoryId}",
                            "publisherId": "#{publisherId}"                           
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

    private val getAllFeeds =
        exec(
            http("Get All Feeds")
                .get("/feeds")
                .check(status().shouldBe(200)),
        )

    private val publishers =
        CoreDsl.scenario("Feeds")
            .exec(
                createCountry
                    .exec(createLanguage)
                    .exec(createCategory)
                    .exec(createPublisher)
                    .exec(createFeed)
                    .exec(readFeed)
                    .exec(updateFeed)
                    .exec(deleteFeed)
                    .exec(deleteFeed)
                    .exec(deleteCategory)
                    .exec(deleteCountry)
                    .exec(deleteLanguage)
                    .exec(deletePublisher),
                getAllFeeds,
                healthCheck,
            )

    init {
        setUp(
            publishers.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
