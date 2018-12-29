package jasition.matching.domain.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import io.vavr.collection.List
import jasition.matching.domain.*

internal class ReplayEventsTest : StringSpec({
    "Events are re-played incrementally and sequentially" {
        val aggregate1 = TestAggregate()
        val aggregate2 = TestAggregate()
        val aggregate3 = TestAggregate()
        val finalAggregate = TestAggregate()
        val event1 = spyk(TestPrimaryEvent(eventId = EventId(1)))
        val event2 = spyk(TestPrimaryEvent(eventId = EventId(2)))
        val event3 = spyk(TestPrimaryEvent(eventId = EventId(3)))

        every { event1.play(aggregate1) } returns Transaction(aggregate2)
        every { event2.play(aggregate2) } returns Transaction(aggregate3)
        every { event3.play(aggregate3) } returns Transaction(finalAggregate)

        val events: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3)

        replay(initial = aggregate1, events = events) shouldBe finalAggregate
    }
    "Only primary events are re-played" {
        val aggregate = TestAggregate()
        val primaryEvent1 = spyk(TestPrimaryEvent(eventId = EventId(1)))
        val sideEffectEvent2 = spyk(TestEvent(eventId = EventId(2)))
        val primaryEvent3 = spyk(TestPrimaryEvent(eventId = EventId(3)))

        every { primaryEvent1.play(aggregate) } returns Transaction(aggregate)
        every { primaryEvent3.play(aggregate) } returns Transaction(aggregate)

        val events: List<Event<Int, TestAggregate>> = List.of(primaryEvent1, sideEffectEvent2, primaryEvent3)

        replay(initial = aggregate, events = events)

        verify(exactly = 0) {
            sideEffectEvent2.play(aggregate)
        }
        verify(exactly = 1) {
            primaryEvent1.play(aggregate)
            primaryEvent3.play(aggregate)
        }
    }
})