package jasition.matching.domain

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import java.util.function.BiFunction

internal class TransactionTest : StringSpec({
    val event1 = TestEvent(1, EventId(1))
    val event2 = TestEvent(2, EventId(2))
    val event3 = TestEvent(3, EventId(3))
    val event4 = TestEvent(4, EventId(4))

    val originalAggregate = TestAggregate("original")
    val originalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2)
    val originalTransaction = Transaction(originalAggregate, originalEvents)

    "Appends new events after the original" {
        val newTransaction = originalTransaction.append(event3, event4)

        val newEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        newTransaction shouldBe Transaction(originalAggregate, newEvents)
    }
    "Uses new aggregate and appends new events after original with default merger" {
        val newAggregate = TestAggregate("new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        val finalTransaction = originalTransaction.append(Transaction(newAggregate, newEvents))

        val finalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        finalTransaction shouldBe Transaction(newAggregate, finalEvents)
    }
    "Uses old aggregate and appends new events after original with custom merger" {
        val newAggregate = TestAggregate("new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        val finalTransaction = originalTransaction.append(Transaction(newAggregate, newEvents),
            BiFunction { original, _ -> original })

        val finalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        finalTransaction shouldBe Transaction(originalAggregate, finalEvents)
    }
})

private class TestAggregate(val value: String) : Aggregate

private data class TestEvent(
    val aggregateId: Int,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun eventType(): EventType = EventType.PRIMARY
    override fun play(aggregate: TestAggregate): Transaction<Int, TestAggregate> = Transaction(aggregate)
}