package jasition.matching.domain.book.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.spyk
import io.vavr.collection.List
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.aBookId
import jasition.matching.domain.anEventId
import jasition.matching.domain.book.Books
import java.time.Instant

internal class EntriesRemovedToBookEventPropertyTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val event = EntriesRemovedFromBookEvent(
        bookId = bookId,
        whenHappened = Instant.now(),
        eventId = eventId,
        entries = List.of(aBookEntry(eventId = eventId, isQuote = true))
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Side-effect event" {
        event.isPrimary() shouldBe false
    }
})

internal class `Given entries are removed from a book` : StringSpec({
    val bookId = aBookId()
    val eventId = anEventId()
    val entry = aBookEntry(eventId = eventId)
    val originalBooks = spyk(Books(bookId))
    val newBooks = spyk(Books(bookId))
    every { originalBooks.addBookEntry(entry) } returns newBooks

    "Then the entries are absent in the book " {

    }

})