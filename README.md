# Markov chain
An implementation of a word-based order-1 [markov chaim](https://en.m.wikipedia.org/wiki/Markov_chain) in Kotlin.

This chain can be trained on any english or english-like texts.
After enough training the chain can generate its owm random phrases
based on the connections between words it's learnt during the training.

The chain can be serialized (saved) and then deserialized (loaded) without any losses.

# Structure
There are two subprojecta:

* **lib** - the library itself
* **examples** - currently contains one example project using the chain.
	The example downloads N random extracts from Wikipedia™, trains a markov
	chain on them and then generates random phrases.

# But why?
Well... I was just sitting and minding my own buisiness when my brain suddenly thought:
"i must create a markov chain and use it in my discord bot"! And now here i am,
writing this text at 4:06 AM after finishing the project. Help me.
