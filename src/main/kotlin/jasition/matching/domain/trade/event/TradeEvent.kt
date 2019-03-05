package jasition.matching.domain.trade.event

import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.verifyEventId
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant
import java.util.function.Function

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
    override fun play(aggregate: Books): Books {
        aggregate verifyEventId eventId

        return updateOrRemoveEntry(updateOrRemoveEntry(aggregate, aggressor), passive)
    }

    private fun updateOrRemoveEntry(
        aggregate: Books,
        entry: TradeSideEntry
    ): Books {
        return if (entry.status.isFinal())
            aggregate.removeBookEntry(
                eventId = eventId,
                side = entry.side,
                bookEntryKey = entry.toBookEntryKey()
            )
        else
            aggregate.updateBookEntry(eventId = eventId,
                side = entry.side,
                bookEntryKey = entry.toBookEntryKey(),
                updater = Function {
                    it.copy(
                        sizes = entry.sizes,
                        status = entry.status
                    )
                })
    }
}

data class TradeSideEntry(
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val isQuote: Boolean,
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


