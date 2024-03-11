package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.AGENT_NAME
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.META_SERVICE_URL
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

class PublisherCRUDSimulation : Simulation() {
    companion object {
        private val publisherName = randomString(5)
        private val languageCode = randomLanguageCode()
        private val countryCode = randomCountryCode()

        val createPublisher =
            exec(
                http("Create Publisher")
                    .post("/publishers")
                    .body(
                        StringBody(
                            """
                                {
                                "name": "$publisherName", 
                                "countryCode": "$countryCode", 
                                "mainLanguageCode":"$languageCode"
                            }
                            """.trimIndent(),
                        ),
                    )
                    .check(status().shouldBe(201))
                    .check(jsonPath("$.name").shouldBe(publisherName))
                    .check(jsonPath("$.countryCode").shouldBe(countryCode))
                    .check(jsonPath("$.mainLanguageCode").shouldBe(languageCode))
                    .check(jsonPath("$.id").saveAs("publisherId")),
            )

        val deletePublisher =
            exec(
                http("Delete Publisher")
                    .delete("/publishers/#{publisherId}")
                    .check(status().shouldBe(204)),
            )
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

    private val readPublisher =
        exec(
            http("Read Publisher")
                .get("/publishers/#{publisherId}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(publisherName))
                .check(jsonPath("$.countryCode").shouldBe(countryCode))
                .check(jsonPath("$.mainLanguageCode").shouldBe(languageCode)),
        )

    private val updatePublisher =
        exec(
            http("Update Publisher")
                .put("/publishers/#{publisherId}")
                .body(
                    StringBody(
                        """
                        {
                        "id": "#{publisherId}", 
                        "name": "$publisherName", 
                        "countryCode": "$countryCode", 
                        "mainLanguageCode":"$languageCode"
                        }
                        """.trimIndent(),
                    ),
                )
                .check(status().shouldBe(200)),
        )

    private val getAllPublishers =
        exec(
            http("Get All Publishers")
                .get("/publishers")
                .check(status().shouldBe(200)),
        )

    private val publishers =
        CoreDsl.scenario("Publishers")
            .exec(
                createPublisher.exec(
                    readPublisher,
                ).exec(
                    updatePublisher,
                ).exec(
                    deletePublisher,
                ),
                getAllPublishers,
                healthCheck,
            )

    init {
        setUp(
            publishers.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
