plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dv8tion:JDA:5.0.0-beta.24")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("DiscordMessageBot")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to "io.github.ANDREJ6693.discord_bot_java.Main")
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) {
            it
        } else {
            zipTree(it)
        }
    })

    with(tasks.jar.get())
}