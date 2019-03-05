package jasition.matching.domain.book.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.aBookId
import jasition.matching.domain.anEventId
import jasition.matching.domain.book.Books

internal class EntryAddedToBookEventPropertyTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val bookEntry = aBookEntry()
    val event = EntryAddedToBookEvent(
        eventId = eventId,
        bookId = bookId,
        entry = bookEntry
    )

    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
})

internal class EntryAddedToBookEventTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val bookEntry = aBookEntry()
    val event = EntryAddedToBookEvent(
        eventId = eventId,
        bookId = bookId,
        entry = bookEntry
    )
    val books = mockk<Books>(relaxed = true)

    "Entry is added to the book" {
        event.play(books)

        verify { books.addBookEntry(entry = bookEntry, eventId = eventId) }

        confirmVerified(books)
    }
})