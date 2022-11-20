plugins {
	id("org.jetbrains.kotlin.jvm") version "1.7.21"
	kotlin("plugin.serialization") version "1.7.21"
	application
}

repositories {
	mavenCentral()
}

dependencies {
	//compileOnly(kotlin("stdlib-jdk8"))

	implementation(project(":lib"))
	
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
	implementation("io.ktor:ktor-client-core:2.1.3")
	implementation("io.ktor:ktor-client-cio:2.1.3")
}

application {
	mainClass.set("com.github.mnemotechnician.markov.examples.WikipediaExtractsMarkovKt")
}
tasks.withType<JavaExec>() {
	standardInput = System.`in`
}
