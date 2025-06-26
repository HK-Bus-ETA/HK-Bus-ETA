plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.loohp.hkbuseta"
version = "1.0.0"

tasks.jar.configure {
    manifest {
        attributes(mapOf("Main-Class" to "com.loohp.hkbuseta.notificationserver.NotificationServerKt"))
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    excludes += "META-INF/*.SF"
    excludes += "META-INF/*.DSA"
    excludes += "META-INF/*.RSA"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

sourceSets {
    main {
        output.setResourcesDir("build/classes/main")
    }
    test {
        output.setResourcesDir("build/classes/test")
    }
}

dependencies {
    implementation(libs.guava)
    implementation(libs.firebase.admin)
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.encoding)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.stately.concurrency)
    implementation(libs.stately.concurrent.collections)
    implementation(libs.xmlCore)
    implementation(libs.serialization.xml)
    implementation(projects.shared)
}