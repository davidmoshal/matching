package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import java.util.function.BiFunction

internal class TransactionTest : StringSpec({
    val event1 = TestEvent(eventId = EventId(1))
    val event2 = TestEvent(eventId = EventId(2))
    val event3 = TestEvent(eventId = EventId(3))
    val event4 = TestEvent(eventId = EventId(4))

    val originalAggregate = TestAggregate(value = "original")
    val originalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2)
    val originalTransaction = Transaction(originalAggregate, originalEvents)

    "Appends new events after the original" {
        val newTransaction = originalTransaction.append(event3, event4)

        val newEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        newTransaction shouldBe Transaction(originalAggregate, newEvents)
    }
    "Uses new aggregate and appends new events after original with default merger" {
        val newAggregate = TestAggregate(value = "new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        val finalTransaction = originalTransaction.append(Transaction(newAggregate, newEvents))

        val finalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        finalTransaction shouldBe Transaction(newAggregate, finalEvents)
    }
    "Uses old aggregate and appends new events after original with custom merger" {
        val newAggregate = TestAggregate(value = "new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        val finalTransaction = originalTransaction.append(
            Transaction(newAggregate, newEvents),
            BiFunction { original, _ -> original })

        val finalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3, event4)
        finalTransaction shouldBe Transaction(originalAggregate, finalEvents)
    }
})

