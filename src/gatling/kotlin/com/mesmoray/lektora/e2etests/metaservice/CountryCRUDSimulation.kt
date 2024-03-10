package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomCountryCode
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class CountryCRUDSimulation : Simulation() {
    private val metaServiceUrl = "http://localhost:8080/api/meta-service/v0"

    private val countryCode = randomCountryCode()
    private val countryName = "Deutschland"

    private val httpProtocol =
        http.baseUrl(metaServiceUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader(
                "Gatling",
            )

    private val createCountry =
        exec(
            http("Create Country")
                .post("/countries")
                .body(
                    StringBody("""{"name": "$countryName", "code": "$countryCode"}""".trimIndent()),
                )
                .check(status().shouldBe(201))
                .check(jsonPath("$.code").shouldBe(countryCode))
                .check(jsonPath("$.name").shouldBe(countryName))
                .check(jsonPath("$.code").saveAs("countryCode")),
        )

    private val readCountry =
        exec(
            http("Read Country")
                .get("/countries/#{countryCode}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(countryName)),
        )

    private val updateCountry =
        exec(
            http("Update Country")
                .put("/countries/#{countryCode}")
                .body(StringBody("""{"code": "$countryCode", "name": "$countryName"}"""))
                .check(status().shouldBe(200)),
        )

    private val deleteCountry =
        exec(
            http("Delete Country")
                .delete("/countries/#{countryCode}")
                .check(status().shouldBe(204)),
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
            )

    init {
        setUp(
            countries.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
