package com.stabila.feature.keyboard.suggestion

/**
 * A single node in the prefix Trie used for word suggestions.
 *
 * Children are stored in a [HashMap] for O(1) character lookups.
 * [topSuggestions] is populated **at build time** (not at query time) so that
 * every lookup returns a pre-ranked list with zero allocations per keystroke.
 *
 * The list is capped at [SuggestionEngine.MAX_SUGGESTIONS] entries and is never
 * modified after the Trie is fully loaded.
 */
internal class TrieNode {
    /** Map from the next character to the child node. */
    val children: HashMap<Char, TrieNode> = HashMap(4) // small initial capacity; grows lazily

    /** True when a complete dictionary word terminates at this node. */
    var isTerminal: Boolean = false

    /**
     * Pre-ranked top completions reachable from this node.
     * Because words are inserted in frequency order, the first words to
     * reach any node are already the highest-frequency completions.
     * Populated during [SuggestionEngine.load] and never mutated after that.
     */
    val topSuggestions: MutableList<String> = ArrayList(SuggestionEngine.MAX_SUGGESTIONS)
}
