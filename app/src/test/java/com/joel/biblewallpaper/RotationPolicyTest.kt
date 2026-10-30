package com.joel.biblewallpaper

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RotationPolicyTest {
    private val ids = listOf("a","b","c","d","e")

    @Test fun shuffleShowsEveryCardBeforeRepeating() {
        val random = Random(42)
        var state = RotationPolicy.select(RotationPolicy.State(),"a")
        val firstCycle = mutableListOf(state.current)
        repeat(ids.size-1) {
            state = RotationPolicy.next(state,ids,random)
            firstCycle.add(state.current)
        }
        assertEquals(ids.size,firstCycle.distinct().size)
        assertEquals(ids.toSet(),firstCycle.toSet())
    }
    @Test fun repeatedCyclesAreCompleteAndNeverRepeatAtBoundary() {
        val random = Random(3)
        var state = RotationPolicy.State()
        var last: String? = null
        repeat(30) {
            val cycle = mutableListOf<String?>()
            repeat(ids.size) {
                state = RotationPolicy.next(state,ids,random)
                assertNotEquals(last,state.current)
                last=state.current
                cycle.add(last)
            }
            assertEquals(ids.toSet(),cycle.toSet())
            assertEquals(ids.size,cycle.distinct().size)
        }
    }
    @Test fun previousAndNextReplayActualHistory() {
        val random = Random(12)
        var state = RotationPolicy.State()
        repeat(4) { state=RotationPolicy.next(state,ids,random) }
        val before=state
        val previous=RotationPolicy.previous(state)
        assertEquals(before.history[2],previous.current)
        val restored=RotationPolicy.next(previous,ids,random)
        assertEquals(before,restored)
        assertEquals(before.seen,previous.seen)
    }
    @Test fun singleEmptyAndStartOfHistoryAreSafe() {
        assertEquals(RotationPolicy.State(),RotationPolicy.next(RotationPolicy.State(),emptyList()))
        val single=RotationPolicy.next(RotationPolicy.State(),listOf("one"))
        assertEquals("one",single.current)
        assertEquals(single,RotationPolicy.previous(single))
        assertEquals(single,RotationPolicy.next(single,listOf("one")))
    }
    @Test fun newDownloadsJoinTheUnseenPartOfTheCurrentCycle() {
        val state=RotationPolicy.State(listOf("a","b"),1,setOf("a","b"))
        val updated=RotationPolicy.reconcile(state,listOf("a","b","new"),"b")
        assertEquals("new",RotationPolicy.next(updated,listOf("a","b","new")).current)
    }
    @Test fun deletedCardsAreRemovedFromHistoryAndCycle() {
        val state=RotationPolicy.State(listOf("a","b","c"),2,setOf("a","b","c"))
        val updated=RotationPolicy.reconcile(state,listOf("a","c"),"c")
        assertEquals(listOf("a","c"),updated.history)
        assertEquals(setOf("a","c"),updated.seen)
        assertEquals("a",RotationPolicy.previous(updated).current)
        val removedAll=RotationPolicy.reconcile(state,emptyList(),null)
        assertEquals(RotationPolicy.State(),removedAll)
    }
    @Test fun choosingGalleryCardBranchesHistoryAndCountsAsSeen() {
        val state=RotationPolicy.State(listOf("a","b","c"),1,setOf("a","b","c"))
        val selected=RotationPolicy.select(state,"d")
        assertEquals(listOf("a","b","d"),selected.history)
        assertEquals(setOf("a","b","c","d"),selected.seen)
        assertEquals("e",RotationPolicy.next(selected,ids).current)
    }
    @Test fun restoringStateKeepsHistoryAndRemainingCycle() {
        val state=RotationPolicy.State(listOf("a","c","b"),1,setOf("a","b","c"))
        val restored=RotationPolicy.reconcile(state.copy(history=state.history.toList(),seen=state.seen.toSet()),ids,"c")
        assertEquals(state,restored)
        assertEquals("b",RotationPolicy.next(restored,ids).current)
    }
    @Test fun historyHasBoundedSize() {
        var state=RotationPolicy.State()
        val random=Random(9)
        repeat(1000) { state=RotationPolicy.next(state,ids,random) }
        assertEquals(RotationPolicy.HISTORY_LIMIT,state.history.size)
        assertEquals(state.history.lastIndex,state.position)
        assertTrue(RotationPolicy.previous(state).position>=0)
    }
    @Test fun cleanupNeverRemovesFavoritesOrCurrent() {
        assertEquals(listOf("old"),RotationPolicy.removable(
            listOf("current","new","favorite","old"),setOf("current","favorite"),1))
        assertEquals(emptyList<String>(),RotationPolicy.removable(listOf("favorite"),setOf("favorite"),0))
    }
    @Test fun intervalsUseSupportedMinimumAndValidFlex() {
        assertEquals(listOf(15,30,60,120,240,480,0),RotationIntervals.choices.map { it.minutes })
        assertEquals(30,RotationIntervals.valid(1))
        assertEquals(0,RotationIntervals.valid(0))
        RotationIntervals.choices.filter { it.minutes>0 }.forEach {
            assertTrue(RotationIntervals.flex(it.minutes)>=5)
            assertTrue(RotationIntervals.flex(it.minutes)<=it.minutes)
        }
    }
}
