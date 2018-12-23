package jasition.matching.domain.trade

import arrow.core.Tuple2
import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.matching.domain.Event
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.trade.event.TradeEvent
import jasition.matching.domain.trade.event.play

fun match(
    aggressor: BookEntry,
    books: Books,
    events: List<Event> = List.empty()
): Tuple2<BookEntry, Transaction<Books>> {
    val limitBook = aggressor.side.oppositeSideBook(books)

    if (notAvailableForTrade(aggressor)
        || limitBook.isEmpty()
    ) {
        return Tuple2(aggressor, Transaction(books, events))
    }

    val passive =
        findNextMatch(aggressor, limitBook.entries.values()) ?: return Tuple2(aggressor, Transaction(books, events))
    val tradeSize = getTradeSize(aggressor, passive)
    val tradePrice = findTradePrice(aggressor, passive)
        ?: throw IllegalArgumentException("Cannot match two entries without price")

    val event = TradeEvent(
        bookId = books.bookId,
        eventId = books.lastEventId.next(),
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

fun findNextMatch(
    aggressor: BookEntry,
    passives: Seq<BookEntry>,
    offset: Int = 0
): BookEntry? {
    if (offset >= passives.size()) {
        return null
    }

    val passive = passives.get(offset)

    if (noFirmClientButOfSameFirm(aggressor, passive)
        || sameFirmClientAndSameFirm(aggressor, passive)
        || findTradePrice(aggressor, passive) == null
    ) {
        return findNextMatch(aggressor, passives, offset + 1)
    }

    return if (priceHasCrossed(aggressor, passive)) passive else null
}