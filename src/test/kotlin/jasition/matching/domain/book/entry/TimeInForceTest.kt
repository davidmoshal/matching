package jasition.matching.domain.book.entry

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.MatchingResult
import kotlin.random.Random

internal class TimeInForceTest : StringSpec({
    "Good-till-cancel allows entries to stay on book if available size is positive"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntrySizes(available = 1)) shouldBe true
    }
    "Good-till-cancel disallows entries to stay on book if available size is zero"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntrySizes(available = 0)) shouldBe false
    }
    "Good-till-cancel adds entries to book if available size is positive"{
        val aggressor = aBookEntry(sizes = EntrySizes(available = 1))
        val books = aBooks(aBookId())
        val existingEvents = List.of<Event<BookId, Books>>(anOrderPlacedEvent())
        val result = MatchingResult(aggressor, Transaction(books, existingEvents))

        TimeInForce.GOOD_TILL_CANCEL.finalise(result) shouldBe Transaction(
            books.addBookEntry(aggressor),
            existingEvents.append(EntryAddedToBookEvent(bookId = books.bookId,
                eventId = EventId(1),
                entry = aggressor
            ))
        )
    }
    "Good-till-cancel does not add entries to book if available size is zero"{
        val aggressor = aBookEntry(sizes = EntrySizes(available = 0))
        val books = aBooks(aBookId())
        val existingEvents = List.of<Event<BookId, Books>>(anOrderPlacedEvent())
        val result = MatchingResult(aggressor, Transaction(books, existingEvents))

        TimeInForce.GOOD_TILL_CANCEL.finalise(result) shouldBe Transaction(
            books,
            existingEvents
        )
    }
    forall(
        row(Random.nextInt(1, 1000), "positive"),
        row(Random.nextInt(1, 1000), "zero")
    ) { availableSize, description ->
        "Immediate-or-cancel disallows entries to stay on book if available size $availableSize is $description"{
            TimeInForce.IMMEDIATE_OR_CANCEL.canStayOnBook(EntrySizes(available = availableSize)) shouldBe false
        }
    }
    "Immediate-or-cancel cancels remaining size if available size is positive"{
        val aggressor = aBookEntry(sizes = EntrySizes(available = 1))
        val books = aBooks(aBookId())
        val existingEvents = List.of<Event<BookId, Books>>(anOrderPlacedEvent())
        val result = MatchingResult(aggressor, Transaction(books, existingEvents))

        TimeInForce.IMMEDIATE_OR_CANCEL.finalise(result) shouldBe Transaction(
            books.copy(lastEventId = books.lastEventId.next()),
            existingEvents.append(
                expectedOrderCancelledByExchangeEvent(
                    entry = aggressor,
                    eventId = books.lastEventId.next(), bookId = books.bookId
                )
            )
        )
    }
    "Immediate-or-cancel does nothing if available size is zero"{
        val aggressor = aBookEntry(sizes = EntrySizes(available = 0))
        val books = aBooks(aBookId())
        val existingEvents = List.of<Event<BookId, Books>>(anOrderPlacedEvent())
        val result = MatchingResult(aggressor, Transaction(books, existingEvents))

        TimeInForce.IMMEDIATE_OR_CANCEL.finalise(result) shouldBe result.transaction
    }
})