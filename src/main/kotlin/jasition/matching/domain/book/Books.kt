package jasition.matching.domain.book

import jasition.matching.domain.Aggregate
import jasition.matching.domain.EventId
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.LocalDate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(Side.BUY),
    val sellLimitBook: LimitBook = LimitBook(Side.SELL),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatuses: TradingStatuses = TradingStatuses(TradingStatus.OPEN_FOR_TRADING),
    val lastEventId: EventId = EventId(0)
) : Aggregate {

    fun addBookEntry(entry: BookEntry): Books = Books(
        bookId = bookId,
        buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.add(entry) else buyLimitBook,
        sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.add(entry) else sellLimitBook,
        businessDate = businessDate,
        tradingStatuses = tradingStatuses,
        lastEventId = entry.key.eventId
    )

    fun traded(entry: TradeSideEntry): Books = Books(
        bookId = bookId,
        buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.update(entry) else buyLimitBook,
        sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.update(entry) else sellLimitBook,
        businessDate = businessDate,
        tradingStatuses = tradingStatuses,
        lastEventId = lastEventId
    )

    fun withEventId(eventId: EventId): Books = Books(
        bookId = bookId,
        buyLimitBook = buyLimitBook,
        sellLimitBook = sellLimitBook,
        businessDate = businessDate,
        tradingStatuses = tradingStatuses,
        lastEventId = eventId
    )

    fun verifyEventId(eventId: EventId): EventId {
        if (!eventId.isNextOf(lastEventId)) {
            throw IllegalArgumentException("Incoming Entry is not the next expected event ID. lastEventId=$lastEventId, incomingEventId=${eventId}")
        }
        return eventId
    }
}
