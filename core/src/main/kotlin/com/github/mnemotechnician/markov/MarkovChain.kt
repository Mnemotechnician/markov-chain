package com.github.mnemotechnician.markov

import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * A simple implementation of a string-bases Markov chain.
 *
 * A markov chain is basically a graph that contains nodes connected to each other.
 * Each connection has a starting node, a destination node (which can be an end node) and a "weight",
 * which represents the chance of choosing to follow this exact connection.
 * 
 * When this chain "trains", it analyzes the connections between words
 * in the sentences of the dataset and builds a graph. For example, if you train it on the
 * phrases "hello world!" and "world is cruel.", it "remembers" the following:
 * 
 * ```
 * A sentence can begin with the words 'hello' or 'world'. The word 'hello'
 * is always followed by 'world'. 'World' is followed by either 'is' or '!'.
 * 'Is' is always followed by 'cruel', which is followed by '.'. '!' and '.'
 * always end the sentence.
 * ```
 *
 * As it "trains" more, it "learns" more possible connections betweem words,
 * which allows to generate more complex (but not always comprehensible) sentences.
 *
 * When this chain generates a string, it begins at one of the beginning nodes
 * it knows. Then it randomly chooses one of the current node's connections,
 * giving precedence to the ones with  a greater weight (i.e. the ones that
 * were more common in the datasets it was trained on), and traverses it.
 * Then it continues to traverse the graph until it either meets a terminal
 * node or exceeds the limit pf traverses.
 *
 * Note: this class is not fully thread-safe.
 * When accessing it from multiple threads, do not try to modify the internal
 * graph manually.
 */
open class MarkovChain {
	/**
	 * A list of all nodes that can serve as a beginning of a sentence.
	 * All nodes present in it must also be present in [nodePool].
	 */
	val beginnings = HashSet<Node>(50)
	/**
	 * All nodes used by this chain.
	 * Do not modify from other threads while generating a string or training the chain!
	 *
	 * All keys of this map are lowercase.
	 */
	val nodePool = HashMap<String, Node>(500)

	/**
	 * A special node that always returns null as the next node.
	 */
	val terminalNode = object : Node("", mutableListOf()) {
		override fun next(random: Random) = null
		override fun connectionForContent(content: String) = error("why would one do that?")
	}

	/** Punctuation marks that end a sentence. */
	protected val punctuationEnd = mutableListOf('.', '!', '?')
	/** Punctuation marks that do not follow a space. */
	protected val punctuationLeft = mutableListOf('.', ',', ':', ';', '!', '?', ')', ']')
	/** Punctuation marks that are not followed by a space. */
	protected val punctuationRight = mutableListOf('(', '[')

	/** Either a whitespace or nothing with a punctuation mark after/before it. */
	protected val whitespaceRegex = """(\s+|(?=[\.,\?!:;/()\[\]])|(?<=[\.,\?!:;/()\[\]]))""".toRegex()
	/** Meaningless stuff, which is stripped from the dataset. May be replaced to better suit the task. */
	protected var meaninglessRegex = "(https?://(\\S+)|<.*>|```.+```|`.+`)".toRegex()
	/** Multi-char punctuation marks that have almost the same meaning as a dot. */
	protected val dotVariationsRegex = """(!|\?|\.){1,}""".toRegex()

	/**
	 * Trains this chain using the specified dataset.
	 * Does not override the results of previous trainings.
	 */
	open fun train(vararg dataset: String) = train(dataset.toList())

	/**
	 * Trains this chain using the specified dataset.
	 * Does not override the results of previous trainings.
	 */
	open fun train(dataset: Collection<String>) = synchronized(this) {
		dataset.mapNotNull { string ->
			string
				.replace(meaninglessRegex, "")
				.replace(dotVariationsRegex, ".") // we can't process multi-char punctuation marks
				.trim()
				.split(whitespaceRegex)
				.filter { it.isNotBlank() }
				.takeIf { it.isNotEmpty() }
		}.forEach { sentence ->
			var last = nodeForContent(sentence.first()).also {
				beginnings.add(it)
			}
			for (i in 1 until sentence.size) {
				val next = last.connectionForContent(sentence[i]).apply { weight += 1U }.node

				// if the previous node is an ending punctuation mark. then the next one is also the beginning of a sentence.
				val isSentence = next.content.length > 1 || next.content.first() !in punctuationLeft
				if (isSentence && last.content.length == 1 && last.content.first() in punctuationEnd) {
					beginnings.add(next)
				}

				last = next
			}
			// the end of the sentence should be connected to the terminal node
			last.connectionForNode(terminalNode).weight += 1U
		}
	}

	/**
	 * Generates a string using a trained chain. 
	 * Throws [IllegalStateException] if the chain has not been trained yet.
	 *
	 * @param limit the maximum number of tokens the resulting string can contain.
	 */
	open fun generate(
		limit: Int = 100,
		random: Random = Random(System.currentTimeMillis())
	) = synchronized(this) {
		require(beginnings.isNotEmpty()) { "The chain has not been trained yet." }
		require(limit >= 1) { "Limit must be >= 1: $limit < 1." }

		buildString {
			var node: Node? = beginnings.random(random)
			var count = 0
			
			while (node != null && count++ < limit) {
				append(node.content)
				node = node.next(random)?.also { next ->
					// append a space if neccessary
					val leftPunct = next.content.length == 1 && next.content[0] in punctuationLeft
					val rightPunct = node!!.content.length == 1 && node!!.content[0] in punctuationRight

					if (!leftPunct && !rightPunct) {
						append(' ')
					}
				}
			}
		}
	}

	/** Returns a pooled [Node] with the specified content (case-insensetive). */
	open fun nodeForContent(content: String) = nodePool.getOrPut(content.lowercase()) {
		Node(content, mutableListOf())
	}

	/** Represents a markov chain node. */
	open inner class Node(
		val content: String,
		val connections: MutableList<Connection>
	) {
		/**
		 * Returns a random node from the list of [connections].
		 * Connections with greater [Connection.weight] are more likely to be returned.
		 */
		open fun next(random: Random): Node? {
			var remaining = connections.fold(0U) { acc, c -> acc + c.weight }

			connections.forEach {
				if (random.nextUInt(1U..remaining) <= it.weight) {
					return it.node
				}
				remaining -= it.weight
			}
			error("") // can never be reached
		}

		/**
		 * Returns a connection to a node with the specified content.
		 * Creates such connection (with no weight) if it didn't exist before.
		 */
		open fun connectionForContent(content: String) = connections.find { it.node.content.equals(content, true) }
			?: Connection(nodeForContent(content), 0U).also {
				connections.add(it)
			}

		/**
		 * Returns a connection to the specified node,
		 * creating it if it didn't exist before.
		 */
		open fun connectionForNode(node: Node) = connections.find { it.node == node }
			?: Connection(node, 0U).also {
				connections.add(it)
			}

		override fun toString() = "Node($content -> ${connections.map { it.node.content }})"

		override fun hashCode(): Int {
			var hash = content.hashCode() * 31
			connections.forEach {
				hash = (hash * 31) xor it.node.content.hashCode() xor it.weight.hashCode()
			}
			return hash
		}

		override fun equals(other: Any?) =
			other is Node && other.content == content && other.connections == connections
	}

	/**
	 * Represents a connection of one node to another.
	 * Connections are monodirectional.
	 */
	data class Connection(
		val node: Node,
		var weight: UInt
	) {
		override fun equals(other: Any?) = 
			other is Connection && other.weight == weight && other.node.content == node.content
	}

	companion object {}
}
