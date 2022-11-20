package com.github.mnemotechnician.markov.examples

import com.github.mnemotechnician.markov.MarkovChain
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.cio.CIO
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*

private const val cancelMessage = "<CANCEL>"

/**
 * An example class that downloads random extracts from wikipedia,
 * trains a markov chain on them and then generates random strings.
 */
class WikipediaExtractsMarkov {
	val client = HttpClient(CIO) {
		install(HttpRequestRetry) {
			exponentialDelay()
		}
	}
	val chain = MarkovChain()

	@OptIn(ObsoleteCoroutinesApi::class)
	suspend fun main() {
		println("===== Markov chain example: wikipedia extracts")
		println()

		var pages = 0U
		do {
			print("Number of pages: ")
			readLine()?.toUIntOrNull()?.let { pages = it }
		} while (pages < 1U)

		println("Will download $pages wikipedia page extracts.")

		coroutineScope {
			val trainer = actor<String>(capacity = Channel.UNLIMITED) {
				for (msg in channel) {
					if (msg == cancelMessage) break

					chain.train(msg)
				}
			}
			// downloader & printer
			launch {
				val pagesNumLength = "$pages".length

				for (i in 0U until pages) {
					print("Downloading... ")
					print("${(i.toFloat() / pages.toFloat() * 100).roundToInt()}% ".padStart(5))
					print("($i/$pages)".padStart(pagesNumLength * 2 + 3).padEnd(10))
					print('\r')

					val pageUrl = client
						.get("https://en.wikipedia.org/api/rest_v1/page/random/summary")
						.bodyAsText()
						.let { Json.parseToJsonElement(it) }
						.jsonObject["uri"]
						?.jsonPrimitive
						?.content
						?.let { 
							"https://en.wikipedia.org/api/rest_v1/page" + it.substringAfterLast("random/..")
						} ?: continue
					val extract = client
						.get(pageUrl)
						.bodyAsText()
						.let { Json.parseToJsonElement(it) }
						.jsonObject["extract"]
						?.jsonPrimitive
						?.content ?: continue
					trainer.send(extract)
				}
				trainer.send(cancelMessage)
			}
		}

		println("Finished training the newtwork.")
		println("===== Press enter to generate a phrase. Type 'quit' to exit.")
		println()

		while (true) {
			print("> ")
			if (readLine()?.lowercase() == "quit") return
			
			println(chain.generate())
			println()
		}
	}
}

fun main() = runBlocking {
	WikipediaExtractsMarkov().main()
}
