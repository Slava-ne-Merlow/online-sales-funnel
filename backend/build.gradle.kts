import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allOpen)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.jpa)
    alias(libs.plugins.spring.kotlin)
    alias(libs.plugins.jacoco)
    alias(libs.plugins.openapi.generator)
}

group = "de.vyacheslav.kushchenko"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(libs.bundles.kotlin)


    implementation(libs.micrometer.prometheus)

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.starter.mail)

    implementation(libs.liquibase)

    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)

    implementation(libs.springdoc.scalar)
    implementation(libs.springdoc.ui)

    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker)

    runtimeOnly(libs.postgres)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.mockk)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.outputLocation = file("jacoco")
    }

    doLast {
        println("Test Coverage Report: file://${rootDir}/jacoco/index.html")
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                // пока ничего тут нет :)
            }
        })
    )
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/src/main/resources/openapi/openapi.yml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)
    apiPackage.set("de.vyacheslav.kushchenko.sales.funnel.api")
    modelPackage.set("de.vyacheslav.kushchenko.sales.funnel.api.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "serializationLibrary" to "jackson",
            "useTags" to "true"
        )
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.openApiGenerate)
}

sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/src/main/kotlin"))

jacoco {
    toolVersion = "0.8.12"
    reportsDirectory = layout.projectDirectory.dir("jacoco")
}
