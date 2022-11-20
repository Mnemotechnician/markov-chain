package com.github.mnemotechnician.markov

import kotlin.test.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class MarkovTest {
	val dataset = listOf(
		"Hello world!", "Today is monday.", "World is beautiful!", "Hello, death, my old friend.",
		"Rawr x3 nuzzles pounces on you uwu you so warm", "Tonight is top story", "It's a me, dio",
		"There's two types of people: those who don't read random strings in the source code and you.",
		"Why am i doing this? What's the point?", "Hello? Is anyone here?",
		"Knock-knock!", "Who's there?", "I am the person you're writing to!"
	)

	@Test
	fun `a trained chain must produce expected results`() {
		val chain = MarkovChain()

		chain.train("Hello world!", "Today is monday.", "World is beautiful!", "Hello, death, my old friend.")

		for (i in 0..10) {
			val str = chain.generate()
			assert(str.length >= 5) { "the chain has not generated the expected result. Received $str." }
		}
	}

	@Test
	fun `check the time required tp train a chain`() {
		print(measureTimeMillis {
			MarkovChain().train(dataset)
		})
		print(" ms")
	}

	@Test
	fun `using the same random seed must produce the same results`() {
		val random1 = Random(1L)
		val random2 = Random(1L)

		val chain = MarkovChain().also { it.train(dataset) }

		for (i in 0..10) {
			assertEquals(
				chain.generate(random = random1),
				chain.generate(random = random2),
				"The same random seed produces different results"
			)
		}
	}

	@Test
	fun `a chain must retain its graph after being serialized and deserialized`() {
		val chain = MarkovChain().also { it.train(dataset) }

		val serialized = chain.serializeToString()
		val deserialized = MarkovChain.deserializeFromString(serialized)

		for (i in 0..10) {
			assert(deserialized.generate().length > 1) { "The deserialized chain generates invalid strings" }
		}

		assert(deserialized.nodePool.values.containsAll(chain.nodePool.values)) { "Node pools do mot match" }
		assert(deserialized.beginnings.containsAll(chain.beginnings)) { "Beginning nodes do not match" }

		// todo: the order of nodes in the chain is not preserved. there's no reliable way to validate their identity.

		//val seed = System.currentTimeMillis()
		//val (random1, random2) = Random(seed) to Random(seed)
		//
		//for (i in 0..10) {
		//	assertEquals(
		//		chain.generate(random = random1),
		//		deserialized.generate(random = random2),
		//		"The result of the deserialized chain does not match that of the original one."
		//	)
		//}
	}
}
