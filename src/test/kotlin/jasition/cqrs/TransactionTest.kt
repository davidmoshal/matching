package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.anEventId

internal class TransactionTest : StringSpec({
    val event1 = TestEvent(eventId = EventId(1))
    val event2 = TestEvent(eventId = EventId(2))
    val event3 = TestEvent(eventId = EventId(3))
    val event4 = TestEvent(eventId = EventId(4))

    val originalAggregate = TestAggregate(value = "original")
    val originalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2)
    val originalTransaction = Transaction(originalAggregate, originalEvents)

    "Appends new events after the original" {
        originalTransaction.append(event3, event4) shouldBe Transaction(
            originalAggregate,
            List.of<Event<Int, TestAggregate>>(event1, event2, event3, event4)
        )
    }
    "When appending transaction, uses new aggregate and appends new events after original events" {
        val newAggregate = TestAggregate(value = "new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        originalTransaction.append(Transaction(newAggregate, newEvents)) shouldBe Transaction(
            newAggregate,
            List.of<Event<Int, TestAggregate>>(event1, event2, event3, event4)
        )
    }
    "Playing an event after a transaction uses new aggregate and appends new events after the event played after the original events" {
        val newAggregate = TestAggregate(value = "new")
        val event = TestPrimaryEvent2(
            updatedAggregate = newAggregate,
            sideEffectEvent = event3, eventId = anEventId()
        )

        originalTransaction.thenPlay(event) shouldBe Transaction(newAggregate, originalEvents.append(event).append(event3))
    }
})

