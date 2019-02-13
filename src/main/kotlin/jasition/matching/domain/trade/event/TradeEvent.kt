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
    override fun isPrimary(): Boolean = false

    override fun play_2_(aggregate: Books): Books {
        aggregate.verifyEventId(eventId)

        return updateBooks(aggregate, passive)
    }

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        aggregate.verifyEventId(eventId)

        return Transaction(updateBooks(aggregate, passive))
    }

    private fun updateBooks(aggregate: Books, sideEntry: TradeSideEntry): Books =
        if (sideEntry.timeInForce.canStayOnBook(sideEntry.sizes))
            aggregate.updateBookEntry(eventId = eventId,
                side = sideEntry.side,
                bookEntryKey = sideEntry.toBookEntryKey(),
                updater = Function {
                    it.copy(
                        sizes = sideEntry.sizes,
                        status = sideEntry.status
                    )
                })
        else aggregate.removeBookEntry(
            eventId = eventId,
            side = sideEntry.side,
            bookEntryKey = sideEntry.toBookEntryKey()
        )
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


