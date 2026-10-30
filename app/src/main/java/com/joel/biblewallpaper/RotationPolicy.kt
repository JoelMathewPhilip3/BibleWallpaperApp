package com.joel.biblewallpaper

object RotationPolicy {
    /** Newest-first input; move through the whole collection and wrap. */
    fun next(ids: List<String>, current: String?): String? {
        if (ids.isEmpty()) return null
        val index = ids.indexOf(current)
        return ids[if (index < 0) 0 else (index + 1) % ids.size]
    }
    /** Keep pinned favorites and the current card; trim only unprotected history. */
    fun removable(ids: List<String>, protected: Set<String>, keep: Int): List<String> =
        ids.filterNot { it in protected }.drop(keep.coerceAtLeast(0))
}
