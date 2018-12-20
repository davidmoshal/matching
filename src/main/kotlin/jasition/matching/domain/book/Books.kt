package jasition.matching.domain.book

import jasition.matching.domain.Aggregate
import jasition.matching.domain.EventId
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Side
import java.time.LocalDate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(false),
    val sellLimitBook: LimitBook = LimitBook(true),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatuses: TradingStatuses = TradingStatuses(TradingStatus.OPEN_FOR_TRADING),
    val lastEventId: EventId = EventId(0)
) : Aggregate {

    fun addBookEntry(
        entry: BookEntry
    ): Books {
        verifyEventId(entry.key.eventId)

        return Books(
            bookId = bookId,
            buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.add(entry) else buyLimitBook,
            sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.add(entry) else sellLimitBook,
            businessDate = businessDate,
            tradingStatuses = tradingStatuses,
            lastEventId = entry.key.eventId
        )
    }

    operator fun plus(eventId: EventId): Books {
        verifyEventId(eventId)

        return Books(
            bookId = bookId,
            buyLimitBook = buyLimitBook,
            sellLimitBook = sellLimitBook,
            businessDate = businessDate,
            tradingStatuses = tradingStatuses,
            lastEventId = eventId
        )
    }

    private fun verifyEventId(eventId: EventId) {
        if (!eventId.isNextOf(lastEventId)) {
            throw IllegalArgumentException("Incoming Entry is not the next expected event ID. lastEventId=$lastEventId, incomingEventId=${eventId}")
        }
    }
}

