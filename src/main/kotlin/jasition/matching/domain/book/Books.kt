package jasition.matching.domain.book

import arrow.core.Tuple2
import io.vavr.collection.List
import jasition.matching.domain.Aggregate
import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.BookEntryKey
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.order.event.TradeEvent
import jasition.matching.domain.order.event.TradeSideEntry
import jasition.matching.domain.order.event.play
import java.lang.Integer.min
import java.time.LocalDate
import java.util.function.BiPredicate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(Side.BUY),
    val sellLimitBook: LimitBook = LimitBook(Side.SELL),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatuses: TradingStatuses = TradingStatuses(TradingStatus.OPEN_FOR_TRADING),
    val lastEventId: EventId = EventId(0)
) : Aggregate {

    fun addBookEntry(entry: BookEntry): Transaction<Books> = Transaction(
        Books(
            bookId = bookId,
            buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.add(entry) else buyLimitBook,
            sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.add(entry) else sellLimitBook,
            businessDate = businessDate,
            tradingStatuses = tradingStatuses,
            lastEventId = verifyEventId(entry.key.eventId)
        )
    )

    fun match(entry: BookEntry): Tuple2<BookEntry, Transaction<Books>> {
        verifyEventId(entry.key.eventId)

        return match(entry, filterByPrice(entry), List.empty())
    }

    fun traded(entry: TradeSideEntry): Books {
        return Books(
            bookId = bookId,
            buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.update(entry) else buyLimitBook,
            sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.update(entry) else sellLimitBook,
            businessDate = businessDate,
            tradingStatuses = tradingStatuses,
            lastEventId = lastEventId
        )
    }

    private fun filterByPrice(entry: BookEntry): Books {
        return Books(
            bookId = bookId,
            buyLimitBook = if (Side.BUY == entry.side) buyLimitBook else filterByPrice(entry, buyLimitBook),
            sellLimitBook = if (Side.SELL == entry.side) sellLimitBook else filterByPrice(entry, sellLimitBook),
            businessDate = businessDate,
            tradingStatuses = tradingStatuses,
            lastEventId = lastEventId
        )
    }

    private fun filterByPrice(entry: BookEntry, book: LimitBook): LimitBook =
        LimitBook(book.entries.filter(BiPredicate(function = fun(
            otherKey: BookEntryKey,
            otherEntry: BookEntry
        ): Boolean {
            if (entry.key.price == null) {
                if (otherEntry.key.price == null) {
                    return false
                }

                return true
            } else if (otherKey.price == null) {
                return true
            }

            return entry.side.comparatorMultipler() * entry.key.price.value.compareTo(otherKey.price.value) <= 0
        })))

    private fun match(
        aggressor: BookEntry,
        books: Books,
        events: List<Event>
    ): Tuple2<BookEntry, Transaction<Books>> {
        val limitBook = aggressor.side.oppositeSideBook(books)

        if (aggressor.size.availableSize <= 0 || limitBook.entries.isEmpty) {
            return Tuple2(aggressor, Transaction(books, events))
        }

        val passive = limitBook.entries.first()._2!!

        val tradeSize = min(aggressor.size.availableSize, passive.size.availableSize)
        val tradePrice = (passive.key.price
            ?: aggressor.key.price
            ?: throw java.lang.IllegalArgumentException("Cannot match two entries without price"))

        val event = TradeEvent(
            bookId = bookId,
            eventId = lastEventId.next(),
            size = tradeSize,
            price = tradePrice,
            whenHappened = aggressor.key.whenSubmitted,
            aggressor = aggressor.toTradeSideEntry(tradeSize),
            passive = passive.toTradeSideEntry(tradeSize)
        )

        val result = play(event, books)

        return match(
            aggressor = aggressor.traded(tradeSize, tradePrice),
            books = result.aggregate,
            events = events.append(event).appendAll(result.events)
        )
    }

    operator fun plus(eventId: EventId): Books = Books(
        bookId = bookId,
        buyLimitBook = buyLimitBook,
        sellLimitBook = sellLimitBook,
        businessDate = businessDate,
        tradingStatuses = tradingStatuses,
        lastEventId = verifyEventId(eventId)
    )

    fun verifyEventId(eventId: EventId): EventId {
        if (!eventId.isNextOf(lastEventId)) {
            throw IllegalArgumentException("Incoming Entry is not the next expected event ID. lastEventId=$lastEventId, incomingEventId=${eventId}")
        }
        return eventId
    }
}
