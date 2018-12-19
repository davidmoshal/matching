package jasition.matching.domain.book

import jasition.matching.domain.Aggregate
import jasition.matching.domain.EventId
import jasition.matching.domain.order.Side
import java.time.LocalDate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(false),
    val sellLimitBook: LimitBook = LimitBook(true),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatus: TradingStatus = TradingStatus.OPEN_FOR_TRADING,
    val lastEventId: EventId = EventId(0)
) : Aggregate {

    fun addBookEntry(
        entry: BookEntry
    ): Books {
        if (lastEventId.compareTo(entry.key.eventId) >= 0) {
            throw IllegalArgumentException("Incoming Entry has a lower event ID. lastEventId=$lastEventId, incomingEventId=${entry.key.eventId}")
        }

        return Books(
            bookId = bookId,
            buyLimitBook = if (Side.BUY.equals(entry.side)) buyLimitBook.add(entry) else buyLimitBook,
            sellLimitBook = if (Side.SELL.equals(entry.side)) sellLimitBook.add(entry) else sellLimitBook,
            businessDate = businessDate,
            tradingStatus = tradingStatus,
            lastEventId = entry.key.eventId
        )
    }
}

