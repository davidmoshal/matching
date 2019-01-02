package jasition.matching.domain.book

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Aggregate
import jasition.cqrs.EventId
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.LocalDate
import java.util.function.Predicate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(Side.BUY),
    val sellLimitBook: LimitBook = LimitBook(Side.SELL),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatuses: TradingStatuses = TradingStatuses(TradingStatus.OPEN_FOR_TRADING),
    val lastEventId: EventId = EventId(0)
) : Aggregate<BookId> {
    override fun aggregateId(): BookId = bookId

    fun addBookEntry(entry: BookEntry): Books = copy(
        buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.add(entry) else buyLimitBook,
        sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.add(entry) else sellLimitBook,
        lastEventId = entry.key.eventId
    )

    fun removeBookEntries(eventId: EventId, entries: Seq<BookEntry>): Books =
        copy(
            buyLimitBook = buyLimitBook.removeAll(entries),
            sellLimitBook = sellLimitBook.removeAll(entries),
            lastEventId = eventId
        )

    fun findBookEntries(predicate: Predicate<BookEntry>): List<BookEntry> =
        List.ofAll(buyLimitBook.entries
            .filterValues(predicate)
            .values()
            .appendAll(sellLimitBook.entries.filterValues(predicate).values()))

    fun traded(entry: TradeSideEntry): Books = copy(
        buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.update(entry) else buyLimitBook,
        sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.update(entry) else sellLimitBook
    )

    fun verifyEventId(eventId: EventId): EventId =
        if (eventId.isNextOf(lastEventId)) eventId
        else throw IllegalArgumentException("Incoming Entry is not the next expected event ID. lastEventId=$lastEventId, incomingEventId=$eventId")
}
