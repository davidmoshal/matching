package jasition.matching.domain.book.event

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.spyk
import io.vavr.collection.List
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class EntryAddedToBookEventPropertyTest : StringSpec({
    val eventId = EventId(1)
    val bookId = BookId("book")
    val event = EntryAddedToBookEvent(
        bookId = bookId,
        whenHappened = Instant.now(),
        eventId = eventId,
        entry = BookEntry(
            price = Price(15),
            whenSubmitted = Instant.now(),
            eventId = EventId(1),
            clientRequestId = ClientRequestId("req1"),
            client = Client(firmId = "firm1", firmClientId = "client1"),
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            size = EntryQuantity(10),
            status = EntryStatus.NEW
        )
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Side-effect event" {
        event.eventType() shouldBe EventType.SIDE_EFFECT
    }
})

internal class `Given an entry is added to an empty book` : StringSpec({
    val bookId = BookId(bookId = "book")
    val entryEventId = EventId(1)
    val entry = BookEntry(
        price = Price(15),
        whenSubmitted = Instant.now(),
        eventId = entryEventId,
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

    "When the event ID is the next event ID of the book, then the entry exists in the book" {
        EntryAddedToBookEvent(
            bookId = bookId,
            whenHappened = Instant.now(),
            eventId = entryEventId,
            entry = entry
        ).play(originalBooks) shouldBe Transaction<BookId, Books>(
            aggregate = newBooks,
            events = List.empty()
        )
    }
    "When a wrong event ID is used in the event, then an exception is thrown" {
        val wrongEventId = entryEventId.next()
        every { originalBooks.verifyEventId(wrongEventId) } throws IllegalArgumentException()

        shouldThrow<IllegalArgumentException> {
            EntryAddedToBookEvent(
                bookId = bookId,
                whenHappened = Instant.now(),
                eventId = wrongEventId,
                entry = entry
            ).play(originalBooks)
        }
    }
    "When a wrong event ID is used in the entry, then an exception is thrown" {
        val wrongEventId = entry.key.eventId
        every { originalBooks.verifyEventId(wrongEventId) } throws IllegalArgumentException()

        shouldThrow<IllegalArgumentException> {
            EntryAddedToBookEvent(
                bookId = bookId,
                whenHappened = Instant.now(),
                eventId = entryEventId.next(),
                entry = entry
            ).play(originalBooks)
        }
    }
})