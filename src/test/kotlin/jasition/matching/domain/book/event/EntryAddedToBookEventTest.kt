package jasition.matching.domain.book.event

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import io.kotlintest.specs.DescribeSpec
import io.mockk.every
import io.mockk.spyk
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class EntryAddedToBookEventPropertyTest : DescribeSpec() {
    init {
        val eventId = EventId(1)
        val bookId = BookId("book")
        val event = EntryAddedToBookEvent(
            bookId = bookId,
            whenHappened = Instant.now(),
            eventId = eventId,
            entry = BookEntry(
                key = BookEntryKey(
                    price = Price(15),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),

                clientRequestId = ClientRequestId("req1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(10),
                status = EntryStatus.NEW
            )
        )
        describe("EntryAddedToBookEvent") {
            it("has Book ID as Aggregate ID") {
                event.aggregateId() shouldBe bookId
            }
            it("has Event ID as Event ID") {
                event.eventId() shouldBe eventId
            }
            it("is a Side-effect event") {
                event.eventType() shouldBe EventType.SIDE_EFFECT
            }
        }
    }
}

internal class EntryAddedToBookEventBehaviourTest : BehaviorSpec() {
    init {
        given("the book is empty") {
            val bookId = BookId(bookId = "book")
            val entryEventId = EventId(1)
            val entry = BookEntry(
                key = BookEntryKey(
                    price = Price(15),
                    whenSubmitted = Instant.now(),
                    eventId = entryEventId
                ),

                clientRequestId = ClientRequestId("req1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(10),
                status = EntryStatus.NEW
            )
            val originalBooks = spyk(Books(bookId))
            val newBooks = spyk(Books(bookId))
            every { originalBooks.addBookEntry(entry) } returns newBooks

            `when`("an entry is added to the book") {
                val result = EntryAddedToBookEvent(
                    bookId = bookId,
                    whenHappened = Instant.now(),
                    eventId = entryEventId,
                    entry = entry
                ).play(originalBooks)

                then("a new book is created with the entry added") {
                    result.aggregate shouldBe newBooks
                    result.events.size() shouldBe 0
                }
            }
            `when`("an entry is added to the book with the wrong Event ID in the event") {
                val wrongEventId = entryEventId.next()
                every { originalBooks.verifyEventId(wrongEventId) } throws IllegalArgumentException()

                then("an IllegalArgumentException is thrown") {
                    shouldThrow<IllegalArgumentException> {
                        EntryAddedToBookEvent(
                            bookId = bookId,
                            whenHappened = Instant.now(),
                            eventId = wrongEventId,
                            entry = entry
                        ).play(originalBooks)
                    }
                }
            }
            `when`("an entry is added to the book with the wrong Event ID in the entry") {
                val wrongEventId = entry.key.eventId
                every { originalBooks.verifyEventId(wrongEventId) } throws IllegalArgumentException()

                then("an IllegalArgumentException is thrown") {
                    shouldThrow<IllegalArgumentException> {
                        EntryAddedToBookEvent(
                            bookId = bookId,
                            whenHappened = Instant.now(),
                            eventId = entryEventId.next(),
                            entry = entry
                        ).play(originalBooks)
                    }
                }
            }
        }
    }
}