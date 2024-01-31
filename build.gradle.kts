plugins {
    id("java")
}

group = "org.onebeastchris"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("org.geysermc.geyser:api:2.2.3-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}