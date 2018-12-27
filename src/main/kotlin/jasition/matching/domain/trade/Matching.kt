package jasition.matching.domain.trade

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.matching.domain.Event
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.trade.event.TradeEvent

fun match(
    aggressor: BookEntry,
    books: Books,
    events: List<Event<BookId, Books>> = List.empty()
): MatchResult {
    val limitBook = aggressor.side.oppositeSideBook(books)

    if (notAvailableForTrade(aggressor.size)
        || limitBook.entries.isEmpty
    ) {
        return MatchResult(aggressor, Transaction(books, events))
    }

    val nextMatch = findNextMatch(aggressor, limitBook.entries.values())
        ?: return MatchResult(aggressor, Transaction(books, events))

    val passive = nextMatch.passive
    val tradeSize = getTradeSize(aggressor.size, passive.size)

    val tradeEvent = TradeEvent(
        bookId = books.bookId,
        eventId = books.lastEventId.next(),
        size = tradeSize,
        price = nextMatch.tradePrice,
        whenHappened = aggressor.key.whenSubmitted,
        aggressor = aggressor.toTradeSideEntry(tradeSize),
        passive = passive.toTradeSideEntry(tradeSize)
    )

    val result = tradeEvent.play(books)

    return match(
        aggressor = aggressor.traded(tradeSize),
        books = result.aggregate,
        events = events.append(tradeEvent).appendAll(result.events)
    )
}

fun findNextMatch(
    aggressor: BookEntry,
    passives: Seq<BookEntry>,
    offset: Int = 0
): Match? {
    if (offset >= passives.size()) {
        return null
    }

    val passive = passives.get(offset)
    val tradePrice = findTradePrice(aggressor.key.price, passive.key.price)

    return if (
        tradePrice == null ||
        sameFirmAndSameFirmClient(aggressor.client, passive.client) ||
        sameFirmButPossibleFirmAgainstClient(aggressor.client, passive.client)
    ) findNextMatch(aggressor, passives, offset + 1)
    else if (priceHasCrossed(aggressor, passive)) Match(passive, tradePrice)
    else null
}

data class Match(val passive: BookEntry, val tradePrice: Price)

data class MatchResult(val aggressor: BookEntry, val transaction: Transaction<BookId, Books>)