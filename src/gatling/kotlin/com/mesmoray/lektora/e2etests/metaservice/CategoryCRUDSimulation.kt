package com.mesmoray.lektora.e2etests.metaservice

import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.AGENT_NAME
import com.mesmoray.lektora.e2etests.config.E2EConfig.Companion.META_SERVICE_URL
import com.mesmoray.lektora.e2etests.metaservice.Utils.Companion.randomString
import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.bodyString
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class CategoryCRUDSimulation : Simulation() {
    companion object {
        private val catName = randomString(5)
        private val catDescription = randomString(10)

        val createCategory =
            exec(
                http("Create Category")
                    .post("/categories")
                    .body(
                        StringBody("""{"name": "$catName", "description": "$catDescription"}""".trimIndent()),
                    )
                    .check(status().shouldBe(201))
                    .check(jsonPath("$.name").shouldBe(catName))
                    .check(jsonPath("$.description").shouldBe(catDescription))
                    .check(jsonPath("$.id").saveAs("categoryId")),
            )

        val deleteCategory =
            exec(
                http("Delete Category")
                    .delete("/categories/#{categoryId}")
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

    private val readCategory =
        exec(
            http("Read Category")
                .get("/categories/#{categoryId}")
                .check(status().shouldBe(200))
                .check(jsonPath("$.name").shouldBe(catName))
                .check(jsonPath("$.description").shouldBe(catDescription)),
        )

    private val updateCategory =
        exec(
            http("Update Category")
                .put("/categories/#{categoryId}")
                .body(StringBody("""{"id": "#{categoryId}","name": "$catName", "description": "$catDescription"}"""))
                .check(status().shouldBe(200)),
        )

    private val getAllCategories =
        exec(
            http("Get All Categories")
                .get("/categories")
                .check(status().shouldBe(200)),
        )

    private val languages =
        CoreDsl.scenario("Categories")
            .exec(
                createCategory.exec(
                    readCategory,
                ).exec(
                    updateCategory,
                ).exec(
                    deleteCategory,
                ),
                getAllCategories,
                healthCheck,
            )

    init {
        setUp(
            languages.injectOpen(CoreDsl.rampUsers(1).during(1)),
        ).protocols(httpProtocol)
    }
}
