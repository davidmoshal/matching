package jasition.cqrs

import arrow.core.Either
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.vavr.collection.List

internal class TransactionTest : StringSpec({
    val event1 = TestEvent(eventId = EventId(1))
    val event2 = TestEvent(eventId = EventId(2))
    val event3 = TestEvent(eventId = EventId(3))
    val event4 = TestEvent(eventId = EventId(4))

    val originalAggregate = TestAggregate(value = "original")
    val originalEvents: List<Event<Int, TestAggregate>> = List.of(event1, event2)
    val originalTransaction = Transaction(originalAggregate, originalEvents)

    "When appending transaction, uses new aggregate and appends new events after original events" {
        val newAggregate = TestAggregate(value = "new")
        val newEvents: List<Event<Int, TestAggregate>> = List.of(event3, event4)

        originalTransaction append Transaction(newAggregate, newEvents) shouldBe Transaction(
            newAggregate,
            List.of<Event<Int, TestAggregate>>(event1, event2, event3, event4)
        )
    }
    "Throws exception if the result is left of Either" {
        val repository = mockk<Repository<Int, TestAggregate>>()

        shouldThrow<Exception> {
            Either.left(Exception("nothing")) commitOrThrow repository
        }
    }
    "Updates aggregate in the repository if the result is right of Either" {
        val repository = spyk<Repository<Int, TestAggregate>>()
        val transaction = Transaction(
            aggregate = originalAggregate,
            events = List.empty()
        )

        Either.right(transaction) commitOrThrow repository

        verify { repository.createOrUpdate(originalAggregate) }
        confirmVerified(repository)
    }
})

