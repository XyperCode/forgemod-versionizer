import org.gradle.jvm.tasks.Jar

plugins {
    java
    application
}

group = "io.github.xypercode"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:1.6")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ModUpdater"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

extensions.getByType(JavaApplication::class).run {
    mainClass.set("ModUpdater")

    applicationDefaultJvmArgs = listOf("-Xmx4G", "-Xms128M")
    applicationName = "ForgeMod Versionizer"
}
