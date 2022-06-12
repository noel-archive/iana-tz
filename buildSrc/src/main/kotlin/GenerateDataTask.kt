/*
 * 🎑 iana-tz-kt: Up to date IANA timezone database library for Kotlin (JVM, JS, Native)
 * Copyright (c) 2022 Noelware <team@noelware.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.noelware.iana.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId

abstract class GenerateDataTask: DefaultTask() {
    init {
        group = "build"
        outputs.upToDateWhen { false }
    }

    private val okhttp = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @get:OutputDirectory
    abstract val outputDirectory: Property<File>

    @TaskAction
    fun execute() {
        project.logger.lifecycle("Grabbing timezone db from https://raw.githubusercontent.com/vvo/tzdb/main/raw-time-zones.json")

        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/vvo/tzdb/main/raw-time-zones.json")
            .method("GET", null)
            .addHeader("Accept", "application/json; charset=utf-8")
            .build()

        try {
            val res = okhttp.newCall(request).execute()
            val body = res.body!!.use { it.string() }

            if (res.code in 200..299) {
                val data = json.decodeFromString(JsonArray.serializer(), body)
                generateWithData(data)
            }
        } catch (e: IOException) {
            project.logger.error("Unable to request to https://raw.githubusercontent.com/vvo/tzdb/main/raw-time-zones.json :(")
        }
    }

    private fun generateWithData(data: JsonArray) {
        val objects = mutableListOf<String>()
        for (item in data.map { it.jsonObject }) {
            val name = item["name"]!!.jsonPrimitive.content
            val altName = item["alternativeName"]!!.jsonPrimitive.content
            val group = item["group"]!!.jsonArray.map { it.jsonPrimitive.content }
            val continentCode = item["continentCode"]!!.jsonPrimitive.content
            val continentName = item["continentName"]!!.jsonPrimitive.content
            val countryCode = item["countryCode"]!!.jsonPrimitive.content
            val countryName = item["countryName"]!!.jsonPrimitive.content
            val mainCities = item["mainCities"]!!.jsonArray.map { it.jsonPrimitive.content }
            val rawOffsetInMinutes = item["rawOffsetInMinutes"]!!.jsonPrimitive.int
            val abbreviation = item["abbreviation"]!!.jsonPrimitive.content

            objects.add("""
            |IANATimezone(
            |    name = "$name",
            |    alternativeName = "$altName",
            |    group = listOf(${group.joinToString(", ") { "\"$it\"" } }),
            |    continentCode = "$continentCode",
            |    continentName = "$continentName",
            |    countryCode = "$countryCode",
            |    countryName = "$countryName",
            |    mainCities = listOf(
            |       ${if (mainCities.size == 1) "\"${mainCities.first()}\"" else mainCities.mapIndexed { i, p -> if (i == 0) "\"$p\"" else "\n\"$p\"" }.joinToString(", ") }
            |    ),
            |    rawOffsetInMinutes = $rawOffsetInMinutes,
            |    abbreviation = "$abbreviation"
            |)
            """.trimMargin())
        }

        val listOfTimezones = PropertySpec.builder("TIMEZONES", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones available as a [List].")
            .initializer("listOf(%L).distinct()", objects.joinToString(", ") { "\n$it" })

        val americanTimezones = PropertySpec.builder("AMERICAS", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `America/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "America/")
            .build()

        val pacificTimezones = PropertySpec.builder("PACIFIC", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Pacific/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Pacific/")
            .build()

        val africanTimezones = PropertySpec.builder("AFRICA", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Africa/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Africa/")
            .build()

        val atlanticTimzones = PropertySpec.builder("ATLANTIC", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Atlantic/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Atlantic/")
            .build()

        val europeanTimezones = PropertySpec.builder("EUROPEAN", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Europe/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Europe/")
            .build()

        val asianTimezones = PropertySpec.builder("ASIA", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Asia/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Asia/")
            .build()

        val indianTimezones = PropertySpec.builder("INDIAN", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Indian/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Indian/")
            .build()

        val antarcticaTimezones = PropertySpec.builder("ANTARCTICA", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Antarctica/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Antarctica/")
            .build()

        val australiaTimezones = PropertySpec.builder("AUSTRALIA", LIST.parameterizedBy(ClassName("org.noelware.iana", "IANATimezone")))
            .addKdoc("Returns all the timezones that are prefixed with `Australia/`")
            .initializer("TIMEZONES.filter { it.name.startsWith(%S) }", "Australia/")
            .build()

        val suppress = listOf("ObjectPropertyName", "ClassName")
        val format = "%S, ".repeat(suppress.size).trimEnd(',')
        val fileSpec = FileSpec.builder("org.noelware.iana", "GeneratedTimezones")
            .addAnnotation(
                AnnotationSpec
                    .builder(JvmName::class)
                    .addMember("%S", "GeneratedTimezonesKt")
                    .build()
            )
            .addAnnotation(
                AnnotationSpec
                    .builder(Suppress::class)
                    .addMember(format, *suppress.toTypedArray())
                    .build()
            )
            .addFileComment("Copyright (c) 2021-%L Noel <cutie@floofy.dev>, Noelware <team@noelware.org>\n", LocalDateTime.now().year)
            .addFileComment("DO NOT EDIT THIS FILE! This was generated by the `./gradlew :generateTzDb` task!")
            .addImport(ClassName("org.noelware.iana", "IANATimezone"), listOf())
            .addProperty(listOfTimezones.build())
            .addProperty(americanTimezones)
            .addProperty(pacificTimezones)
            .addProperty(asianTimezones)
            .addProperty(europeanTimezones)
            .addProperty(antarcticaTimezones)
            .addProperty(africanTimezones)
            .addProperty(atlanticTimzones)
            .addProperty(indianTimezones)
            .addProperty(australiaTimezones)
            .build()

        val directory = outputDirectory.get()
        directory.mkdirs()

        fileSpec.writeTo(directory.toPath())
    }
}
