plugins {
    java
}

group = "eu.praxios"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.16.3")       // API-Nutzung zur Laufzeit (Server hat FAWE)
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.16.3")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.processResources {
    expand("version" to project.version)
}
