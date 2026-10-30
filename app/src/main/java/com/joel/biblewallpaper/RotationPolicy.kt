package com.joel.biblewallpaper

import kotlin.random.Random

/** Immutable shuffle cycle and navigation history, independent of Android. */
object RotationPolicy {
    const val HISTORY_LIMIT = 256
    data class State(val history: List<String> = emptyList(), val position: Int = -1,
        val seen: Set<String> = emptySet()) {
        val current: String? get() = history.getOrNull(position)
    }
    fun reconcile(state: State, ids: List<String>, current: String?): State {
        val valid = ids.toSet()
        val split = (state.position + 1).coerceIn(0,state.history.size)
        val before = state.history.take(split).filter { it in valid }
        val after = state.history.drop(split).filter { it in valid }
        var result = State(before + after,before.lastIndex,state.seen.intersect(valid))
        if (current != null && current in valid && result.current != current) result = select(result,current)
        return result
    }
    fun select(state: State, id: String): State {
        if (state.current == id) return state.copy(seen=state.seen + id)
        val history = (state.history.take(state.position+1) + id).takeLast(HISTORY_LIMIT)
        return State(history,history.lastIndex,state.seen + id)
    }
    fun previous(state: State): State = if (state.position > 0)
        state.copy(position=state.position-1) else state

    fun next(state: State, ids: List<String>, random: Random = Random.Default): State {
        if (ids.isEmpty()) return State()
        // Going back and then forward replays actual viewed cards.
        if (state.position < state.history.lastIndex) return state.copy(position=state.position+1)
        var seen = state.seen.intersect(ids.toSet())
        var candidates = ids.distinct().filterNot { it in seen }
        if (candidates.isEmpty()) {
            // Start a fresh cycle, allowing every card once. Avoid an adjacent repeat.
            seen = emptySet()
            candidates = ids.distinct().filterNot { ids.size > 1 && it == state.current }
        }
        val chosen = candidates.random(random)
        return select(state.copy(seen=seen),chosen)
    }
    fun removable(ids: List<String>, protected: Set<String>, keep: Int): List<String> =
        ids.filterNot { it in protected }.drop(keep.coerceAtLeast(0))
}
