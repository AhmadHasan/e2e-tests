package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.AGENT_NAME
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.META_SERVICE_URL
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomLanguageCode
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class LanguageCRUDSimulation : Simulation() {
    companion object {
        private val languageCode = randomLanguageCode()
        private val languageName = "Deutsch"

        val createLanguage =
            exec(
                http("Create Language")
                    .post("/languages")
                    .body(
                        StringBody("""{"name": "$languageName", "code": "$languageCode"}""".trimIndent()),
                    )
                    .check(status().shouldBe(201))
                    .check(jsonPath("$.code").shouldBe(languageCode))
                    .check(jsonPath("$.name").shouldBe(languageName))
                    .check(jsonPath("$.code").saveAs("languageCode")),
            )

        val deleteLanguage =
            exec(
                http("Delete Language")
                    .delete("/languages/#{languageCode}")
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

    private val readLanguage =
        exec(
            http("Read Language")
                .get("/languages/#{languageCode}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(languageName)),
        )

    private val updateLanguage =
        exec(
            http("Update Language")
                .put("/languages/#{languageCode}")
                .body(StringBody("""{"code": "$languageCode", "name": "$languageName"}"""))
                .check(status().shouldBe(200)),
        )

    private val getAllLanguages =
        exec(
            http("Get All Languages")
                .get("/languages")
                .check(status().shouldBe(200)),
        )

    private val languages =
        CoreDsl.scenario("Languages")
            .exec(
                createLanguage.exec(
                    readLanguage,
                ).exec(
                    updateLanguage,
                ).exec(
                    deleteLanguage,
                ),
                getAllLanguages,
                healthCheck,
            )

    init {
        setUp(
            languages.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
