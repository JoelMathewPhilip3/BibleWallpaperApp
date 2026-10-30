package com.joel.biblewallpaper
import org.junit.Assert.*
import org.junit.Test
class RotationPolicyTest {
    @Test fun rotationVisitsAllCardsBeforeWrapping() {
        val ids = listOf("newest", "middle", "oldest")
        assertEquals("middle", RotationPolicy.next(ids,"newest"))
        assertEquals("oldest", RotationPolicy.next(ids,"middle"))
        assertEquals("newest", RotationPolicy.next(ids,"oldest"))
    }
    @Test fun emptySingleAndDeletedSelectionAreHandled() {
        assertNull(RotationPolicy.next(emptyList(),null))
        assertEquals("only",RotationPolicy.next(listOf("only"),"only"))
        assertEquals("a",RotationPolicy.next(listOf("a","b"),"deleted"))
    }
    @Test fun cleanupNeverRemovesFavoritesOrCurrent() {
        assertEquals(listOf("old"),RotationPolicy.removable(
            listOf("current","new","favorite","old"),setOf("current","favorite"),1))
        assertEquals(emptyList<String>(),RotationPolicy.removable(listOf("favorite"),setOf("favorite"),0))
    }
}
