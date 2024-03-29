package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.AGENT_NAME
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.META_SERVICE_URL
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomCountryCode
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class CountryCRUDSimulation : Simulation() {
    companion object {
        private val countryCode = randomCountryCode()
        private const val COUNTRY_NAME = "Deutschland"

        val createCountry =
            exec(
                http("Create Country")
                    .post("/countries")
                    .body(
                        StringBody("""{"name": "$COUNTRY_NAME", "code": "$countryCode"}""".trimIndent()),
                    )
                    .check(status().shouldBe(201))
                    .check(jsonPath("$.code").shouldBe(countryCode))
                    .check(jsonPath("$.name").shouldBe(COUNTRY_NAME))
                    .check(jsonPath("$.code").saveAs("countryCode")),
            )

        val deleteCountry =
            exec(
                http("Delete Country")
                    .delete("/countries/#{countryCode}")
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

    private val readCountry =
        exec(
            http("Read Country")
                .get("/countries/#{countryCode}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(COUNTRY_NAME)),
        )

    private val updateCountry =
        exec(
            http("Update Country")
                .put("/countries/#{countryCode}")
                .body(StringBody("""{"code": "$countryCode", "name": "$COUNTRY_NAME"}"""))
                .check(status().shouldBe(200)),
        )

    private val getAllCountries =
        exec(
            http("Get All Country")
                .get("/countries")
                .check(status().shouldBe(200)),
        )

    private val countries =
        CoreDsl.scenario("Countries")
            .exec(
                createCountry.exec(
                    readCountry,
                ).exec(
                    updateCountry,
                ).exec(
                    deleteCountry,
                ),
                getAllCountries,
                healthCheck,
            )

    init {
        setUp(
            countries.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
