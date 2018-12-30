package jasition.matching.domain.trade.event

import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

data class TradeEvent(
    val eventId: EventId,
    val bookId: BookId,
    val size: Int,
    val price: Price,
    val whenHappened: Instant,
    val aggressor: TradeSideEntry,
    val passive: TradeSideEntry
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = false

    override fun play(aggregate: Books): Transaction<BookId, Books> = Transaction(
        aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))
            .traded(aggressor)
            .traded(passive)
    )
}

data class TradeSideEntry(
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val entryType: EntryType,
    val side: Side,
    val sizes: EntrySizes,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenSubmitted: Instant,
    val eventId: EventId,
    val status: EntryStatus
) {
    fun toBookEntryKey(): BookEntryKey =
        BookEntryKey(price = price, whenSubmitted = whenSubmitted, eventId = eventId)
}


