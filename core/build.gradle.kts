plugins {
	kotlin("jvm")
	`java-library`
	`maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
	// no dependencies other than the kotlin stdlib
	compileOnly(kotlin("stdlib-jdk8"))

	testImplementation(kotlin("test"))
}

tasks.jar {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from(*configurations.runtimeClasspath.files.map { if (it.isDirectory()) it else zipTree(it) }.toTypedArray())
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "com.github.mnemotechnician"
			artifactId = "markov-chain"
			version = "v1.0"

			from(components["java"])
		}
	}
}

tasks.test {
	outputs.upToDateWhen { false }
	testLogging.showStandardStreams = true
}
