plugins {
    java
    id("com.gradleup.shadow") version "9.3.2"
}

group = "eu.praxios"
version = "0.2.9-alpha.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://maven.enginehub.org/repo/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
val sqliteJdbc = "org.xerial:sqlite-jdbc:3.54.1.0"

dependencies {
    compileOnly(paperApi)
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.16.3")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.16.3")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")

    // Standard-JAR enthält keine Android-Natives mehr; Termux benötigt den zweiten Eintrag.
    implementation(sqliteJdbc)
    implementation("$sqliteJdbc:natives-android")

    testImplementation(paperApi)
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.2")
    testRuntimeOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.16.3")
    testRuntimeOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.16.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    expand("version" to project.version)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
