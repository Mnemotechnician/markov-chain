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
	repositories {
		maven {
			name = "Github"
			url = uri("https://maven.pkg.github.com/mnemotechnician/markov-chain")
			credentials {
				username = findProperty("github.username") as? String
					?: System.getenv("GITHUB_USERNAME")
				password = findProperty("github.token") as? String
					?: System.getenv("GITHUB_TOKEN")
			}
		}
	}
	publications {
		create<MavenPublication>("maven") {
			pom {
				url.set("https://github.com/mnemotechnician/markov-chain.git")
			}
			from(components["java"])

			groupId = "com.github.mnemotechnician"
			artifactId = "markov-chain"
			version = findProperty("version") as? String ?: System.getenv("VERSION") ?: "1.0"
		}
	}
}

tasks.test {
	outputs.upToDateWhen { false }
	testLogging.showStandardStreams = true
}
