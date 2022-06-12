# 🎑 IANA Timezone Library for Kotlin Multiplatform
> *Up to date IANA timezone database library for Kotlin (JVM, JS, Native)*

## Usage
```kotlin
import org.noelware.iana.*

fun main() {
    AMERICAS.first { it.name.endsWith("Phoenix") }
    // => org.noelware.iana.IANATimezone[America/Phoenix]
}
```

## Installation
### Gradle
#### Kotlin DSL
```kotlin
repositories {
    // If you're using the Noel Gradle Utils package, you can use the
    // `noelware` extension
    maven {
        url = uri("https://maven.noelware.org")
    }
}

dependencies {
    // If you're using the Noel Gradle Utils package, you can use
    // the `noelware` extension to automatically prefix `org.noelware.<module>`
    // in the dependency declaration
    implementation("org.noelware.iana:iana-tz:<version>")
}
```

### Groovy DSL
```groovy
repositories {
    maven {
        url "https://maven.noelware.org"
    }
}

dependencies {
    implementation "org.noelware.iana:iana-tz:<version>"
}
```

### Maven
Declare the **Noelware** Maven repository under the `<repositories>` chain:

```xml
<repositories>
    <repository>
        <id>noelware-maven</id>
        <url>https://maven.noelware.org</url>
    </repository>
</repositories>
```

Now declare the dependency you want under the `<dependencies>` chain:

```xml
<dependencies>
    <dependency>
        <groupId>org.noelware.iana</groupId>
        <artifactId>iana-tz-jvm</artifactId>
        <version>{{VERSION}}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

## License
**iana-tz-kt** is released under the **MIT License** with love by Noelware
