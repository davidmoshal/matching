package jasition.matching.domain.order.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import java.time.Instant

internal class OrderCancelledByExchangeEventPropertyTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val event = OrderCancelledByExchangeEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = eventId,
        sizes = anEntrySizes(),
        status = EntryStatus.CANCELLED
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Primary event" {
        event.isPrimary() shouldBe false
    }
})

internal class `Given an order is cancelled by the Exchange` : StringSpec({
    val bookId = aBookId()
    val requestId = aClientRequestId()
    val whoRequested = aFirmWithClient()
    val entry = aBookEntry(eventId = EventId(1), whoRequested = whoRequested, requestId = requestId)

    val entryOfOtherFirmButSameRequestId = entry
        .withKey(eventId = EventId(2))
        .copy(whoRequested = whoRequested.copy(firmId = "something else"))

    val entryOfSameFirmClientButDifferentRequestId = entry
        .withKey(eventId = EventId(3))
        .copy(requestId = requestId.copy(current = "something else"))

    val entryOfDifferentFirmClientAndRequestId = entry
        .withKey(eventId = EventId(4))
        .copy(whoRequested = whoRequested.copy(firmId = "something else"), requestId = requestId.copy(current = "something else"))
    val books = aBooks(bookId)
        .addBookEntry(entry)
        .addBookEntry(entryOfOtherFirmButSameRequestId)
        .addBookEntry(entryOfSameFirmClientButDifferentRequestId)
        .addBookEntry(entryOfDifferentFirmClientAndRequestId)
    val event = OrderCancelledByExchangeEvent(
        requestId = requestId,
        whoRequested = whoRequested,
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = EventId(5),
        sizes = anEntrySizes(),
        status = EntryStatus.CANCELLED
    )

    val actual = event.play(books)

    "The opposite-side book is not affected" {
        actual.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "No side-effect has happened" {
        actual.events.size() shouldBe 0
    }
    "The entry removed from the same-side book" {
        actual.aggregate.buyLimitBook.entries.containsValue(entry) shouldBe false
    }
    "Entries of different firm client not affected" {
        actual.aggregate.buyLimitBook.entries.containsValue(entryOfOtherFirmButSameRequestId) shouldBe true
    }
    "Entries of different request ID not affected" {
        actual.aggregate.buyLimitBook.entries.containsValue(entryOfSameFirmClientButDifferentRequestId) shouldBe true
    }
    "Entries of different request ID and different firm client not affected" {
        actual.aggregate.buyLimitBook.entries.containsValue(entryOfDifferentFirmClientAndRequestId) shouldBe true
    }
})

