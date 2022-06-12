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

import org.noelware.iana.gradle.*
import dev.floofy.utils.gradle.*
import java.io.StringReader
import java.util.Properties

plugins {
    id("org.jetbrains.dokka")
    kotlin("multiplatform")
    `maven-publish`
    `java-library`
}

group = "org.noelware.iana"
version = "$VERSION"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    explicitApi()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JAVA_VERSION.toString()
            kotlinOptions.javaParameters = true
            kotlinOptions.freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    js(BOTH) {
        browser()
        nodejs()

        compilations.all {
            packageJson {
                name = "@noelware/iana-tz"
                private = false

                customField("publicConfig" to mapOf("access" to true))
            }
        }
    }

    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")

    when {
        os.startsWith("Windows") -> {
            mingwX64("native")
        }

        os == "Linux" -> {
            when (arch) {
                "amd64" -> linuxX64("native")
                "arm64" -> linuxArm64("native")
                else -> error("Linux with architecture $arch is not supported.")
            }
        }

        os == "Mac OS X" -> {
            when (arch) {
                "amd64" -> macosX64("native")
                "arm64" -> macosArm64("native")
                else -> error("macOS with architecture $arch is not supported.")
            }
        }

        else -> error("Operating system $os is not supported.")
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src/commonGenerated")
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        val jvmMain by getting
        val jsMain by getting
        val nativeMain by getting
    }
}

val dokkaJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assemble Kotlin documentation with Dokka"

    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
}

tasks {
    create<GenerateDataTask>("generateTzDb") {
        outputDirectory by file("src/commonGenerated")
    }

    dokkaHtml {
        outputDirectory by file("$projectDir/docs")
        dokkaSourceSets.configureEach {
            sourceLink {
                localDirectory.set(projectDir.resolve("src/$name/kotlin"))
                remoteUrl.set(uri("https://github.com/Noelware/iana-db-kt/blob/master/src/$name/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }

            val map = asMap

            if (map.containsKey("jsMain")) {
                named("jsMain") {
                    displayName.set("JS")
                }
            }

            if (map.containsKey("jvmMain")) {
                named("jvmMain") {
                    jdkVersion.set(8)
                    displayName.set("JVM")
                }
            }

            if (map.containsKey("nativeMain")) {
                named("nativeMain") {
                    displayName.set("Native")
                }
            }
        }
    }
}

// Get the `publishing.properties` file from the `gradle/` directory
// in the root project.
val publishingPropsFile = file("${rootProject.projectDir}/gradle/publishing.properties")
val publishingProps = Properties()

// If the file exists, let's get the input stream
// and load it.
if (publishingPropsFile.exists()) {
    publishingProps.load(publishingPropsFile.inputStream())
} else {
    // Check if we do in environment variables
    val accessKey = System.getenv("NOELWARE_PUBLISHING_ACCESS_KEY") ?: ""
    val secretKey = System.getenv("NOELWARE_PUBLISHING_SECRET_KEY") ?: ""

    if (accessKey.isNotEmpty() && secretKey.isNotEmpty()) {
        val data = """
        |s3.accessKey=$accessKey
        |s3.secretKey=$secretKey
        """.trimMargin()

        publishingProps.load(StringReader(data))
    }
}

// Check if we have the `NOELWARE_PUBLISHING_ACCESS_KEY` and `NOELWARE_PUBLISHING_SECRET_KEY` environment
// variables, and if we do, set it in the publishing.properties loader.
val snapshotRelease: Boolean = run {
    val env = System.getenv("NOELWARE_PUBLISHING_IS_SNAPSHOT") ?: "false"
    env == "true"
}

publishing {
    publications {
        filterIsInstance<MavenPublication>().forEach { publication ->
            publication.artifact(dokkaJar.get())
            publication.pom {
                description by "\uD83C\uDF91 Up to date IANA timezone database library for Kotlin (JVM, JS, Native)"
                name by project.name
                url by "https://iana.noelware.org"

                licenses {
                    license {
                        name by "MIT License"
                        url by "https://github.com/Noelware/iana/blob/master/LICENSE"
                    }
                }

                developers {
                    developer {
                        email by "team@noelware.org"
                        name by "Noelware Team"
                        url by "https://noelware.org"
                    }

                    developer {
                        email by "cutie@floofy.dev"
                        name by "Noel"
                        url by "https://floofy.dev"
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/Noelware/iana.git")
                    developerConnection.set("scm:git:https://github.com/Noelware/iana.git")
                    url.set("https://github.com/Noelware/iana")
                }
            }
        }
    }

    repositories {
        val url = if (snapshotRelease) "s3://maven.noelware.org/snapshots" else "s3://maven.noelware.org"
        maven(url) {
            credentials(AwsCredentials::class.java) {
                this.accessKey = publishingProps.getProperty("s3.accessKey") ?: ""
                this.secretKey = publishingProps.getProperty("s3.secretKey") ?: ""
            }
        }
    }
}
