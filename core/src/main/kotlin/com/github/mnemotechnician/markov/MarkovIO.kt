package com.github.mnemotechnician.markov

import java.io.*
import java.util.Base64

const val MARKOV_IO_VERSION = 1

/**
 * Serializes this chain into the providen output stream.
 * This function does not close nor flush the underlying stream.
 */
fun MarkovChain.serializeToStream(stream: OutputStream) = synchronized(this) {
	with(DataOutputStream(stream)) {
		writeInt(MARKOV_IO_VERSION)

		val nodes = nodePool.values
		writeInt(nodePool.size)
		
		// node contents and connection counts - mandatory to create a node.
		nodes.forEach {
			writeUTF(it.content)
			writeInt(it.connections.size)
		}

		// node connections - can only be read after reading the node list.
		nodes.forEach {
			writeInt(it.connections.size) // to make it easier to read, this field is repeated

			it.connections.forEach { connection ->
				writeLong(connection.weight.toLong()) // uh.
				writeInt(nodes.indexOf(connection.node)) // may be -1 if it's the terminal node
			}
		}

		// beginning node indexes
		writeInt(beginnings.size)
		beginnings.forEach {
			writeInt(nodes.indexOf(it))
		}
	}
}

fun MarkovChain.serializeToString(): String = ByteArrayOutputStream().also {
	DataOutputStream(it).use { serializeToStream(it) }
}.toByteArray().let {
	Base64.getEncoder().encodeToString(it)
}

fun MarkovChain.serializeToFile(file: File) = file.outputStream().use { serializeToStream(it) }

fun MarkovChain.Companion.deserializeFromStream(stream: InputStream) = with(DataInputStream(stream)) {
	val version = readInt() 
	if (version > MARKOV_IO_VERSION) {
		error("The version of this markov chain is greater than that of the deserializer: $version > $MARKOV_IO_VERSION.")
	}

	val nodeCount = readInt()
	val nodes = ArrayList<MarkovChain.Node>(nodeCount)

	val chain = MarkovChain()

	for (i in 0 until nodeCount) {
		val content = readUTF()
		val connectionCount = readInt()

		nodes += chain.Node(content, ArrayList<MarkovChain.Connection>(connectionCount))
	}
	for (i in 0 until nodeCount) {
		val node = nodes[i]
		val connectionCount = readInt()

		for (i in 0 until connectionCount) {
			val weight = readLong().toUInt()
			val targetIndex = readInt()
			val targetNode = if (targetIndex == -1) chain.terminalNode else nodes[targetIndex]

			node.connections += MarkovChain.Connection(targetNode, weight)
		}
	}

	nodes.forEach {
		chain.nodePool[it.content.lowercase()] = it
	}
	
	val beginningNodeCount = readInt()
	for (i in 0 until beginningNodeCount) {
		val index = readInt()

		chain.beginnings.add(nodes[index])
	}

	chain
}

fun MarkovChain.Companion.deserializeFromString(string: String) =
	deserializeFromStream(ByteArrayInputStream(
		Base64.getDecoder().decode(string.toByteArray())
	))

fun MarkovChain.Companion.deserializeFromFile(file: File) =
	deserializeFromStream(file.inputStream())
