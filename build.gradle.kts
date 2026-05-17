plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
    id("com.modrinth.minotaur") version "2.+"
}

group = "oxy.geyser.fp"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.geysermc.geyser:core:2.9.5-SNAPSHOT") {
        exclude(group = "com.google.code.gson", module = "gson")
    }

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("org.cloudburstmc.fastutil.commons:long-common:8.5.15")
    implementation("org.cloudburstmc.fastutil.maps:long-object-maps:8.5.15")

}

tasks {
    shadowJar {
        archiveFileName = "geyserfloatingpoints.jar"

        relocate("org.yaml.snakeyaml", "oxy.geyser.fp.shaded.snakeyaml")
        relocate("com.fasterxml", "oxy.geyser.fp.shaded.fasterxml")
        relocate("it.unimi.dsi.fastutil", "oxy.geyser.fp.shaded.fastutil")
    }
}

modrinth {
    token = System.getenv("MODRINTH_TOKEN")
    versionName.set(version.toString() + "-" + getCommitHash())
    versionNumber.set(version.toString())
    projectId = "geyserfloatingpoints"
    versionType = "release"
    uploadFile.set(tasks.getByPath("shadowJar"))

    var releaseNotes = rootProject.file("release_notes.md")
    changelog.set(releaseNotes.exists().let {
        if (it) releaseNotes.readText() else ""
    })

    gameVersions = listOf("1.21.11");
    loaders = listOf("geyser")
}

// Thanks to https://gist.github.com/JonasGroeger/7620911 :tm:
fun getCommitHash(): String {
    val gitFolder = "$projectDir/.git/"
    val takeFromHash = 7
    val head = File(gitFolder + "HEAD").readText().split(":")
    val isCommit = head.size == 1

    if(isCommit) return head[0].trim().take(takeFromHash)

    val refHead = File(gitFolder + head[1].trim())
    return refHead.readText().trim().take(takeFromHash);
}