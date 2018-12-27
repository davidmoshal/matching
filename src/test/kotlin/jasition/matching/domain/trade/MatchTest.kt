package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.mockk
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntryQuantity
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.trade.event.TradeEvent

internal class MatchTest : StringSpec({
    val bookId = aBookId()
    val client = aFirmWithClient(firmId = "firm1", firmClientId = "firmClient1")
    val otherClient = aFirmWithClient(firmId = "firm2", firmClientId = "firmClient2")
    val existingEvents = List.of(mockk<OrderPlacedEvent>(), mockk<TradeEvent>())

    "Stop matching when there is no available size in the aggressor" {
        val books = aBookEntry(side = Side.SELL, client = client)
            .toEntryAddedToBookEvent(bookId)
            .play(Books(bookId))
            .aggregate
        val aggressor = aBookEntry(side = Side.BUY, client = otherClient, size = EntryQuantity(0))
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "Stop matching when there is no more entries in the opposite-side book" {
        val books = aBookEntry(side = Side.BUY, client = client)
            .toEntryAddedToBookEvent(bookId)
            .play(Books(bookId))
            .aggregate
        val aggressor = aBookEntry(side = Side.BUY, client = otherClient)
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
            ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "Stop matching when there is no more next match in the opposite-side book" {
        val books = aBookEntry(side = Side.SELL, client = client, price = Price(35))
            .toEntryAddedToBookEvent(bookId)
            .play(Books(bookId))
            .aggregate
        val aggressor = aBookEntry(side = Side.BUY, client = otherClient, price = Price(30))
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "First match fully filled the aggressor and the passive entry in the opposite-side book" {
        // TODO Examine each field's validity
        val passive = aBookEntry(side = Side.SELL, client = client, price = Price(35))
        val books = passive
            .toEntryAddedToBookEvent(bookId)
            .play(Books(bookId))
            .aggregate
        val aggressor = aBookEntry(side = Side.BUY, client = otherClient, price = Price(35))
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor.traded(aggressor.size.availableSize),
            transaction = Transaction(
                aggregate = Books(bookId).copy(lastEventId = EventId(2)),
                events = existingEvents.append(
                    TradeEvent(
                        eventId = books.lastEventId.next(),
                        bookId = bookId,
                        size = aggressor.size.availableSize,
                        price = Price(35),
                        whenHappened = aggressor.key.whenSubmitted,
                        aggressor = aggressor.copy(
                            size = EntryQuantity(
                                availableSize = 0,
                                tradedSize = aggressor.size.availableSize,
                                cancelledSize = 0
                            )
                        ).toTradeSideEntry(),
                        passive = passive.copy(
                            size = EntryQuantity(
                                availableSize = 0,
                                tradedSize = passive.size.availableSize,
                                cancelledSize = 0
                            )
                        ).toTradeSideEntry()
                    )
                )
            )
        )
    }
})