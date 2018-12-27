package jasition.matching.domain.trade

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.matching.domain.Event
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

fun match(
    aggressor: BookEntry,
    books: Books,
    events: List<Event<BookId, Books>> = List.empty()
): MatchResult {
    val limitBook = aggressor.side.oppositeSideBook(books)

    if (cannotMatchAnyFurther(aggressor, limitBook)) {
        return MatchResult(aggressor, Transaction(books, events))
    }

    val nextMatch = findNextMatch(aggressor, limitBook.entries.values())
        ?: return MatchResult(aggressor, Transaction(books, events))

    val tradeSize = getTradeSize(aggressor.size, nextMatch.passive.size)
    val tradedAggressor = aggressor.traded(tradeSize)
    val tradedPassive = nextMatch.passive.traded(tradeSize)
    val tradeEvent = TradeEvent(
        bookId = books.bookId,
        eventId = books.lastEventId.next(),
        size = tradeSize,
        price = nextMatch.tradePrice,
        whenHappened = aggressor.key.whenSubmitted,
        aggressor = tradedAggressor.toTradeSideEntry(),
        passive = tradedPassive.toTradeSideEntry()
    )
    val result = tradeEvent.play(books)

    return match(
        aggressor = tradedAggressor,
        books = result.aggregate,
        events = events.append(tradeEvent).appendAll(result.events)
    )
}

fun findNextMatch(
    aggressor: BookEntry,
    passives: Seq<BookEntry>,
    offset: Int = 0
): Match? {
    val passive = findPassive(passives, offset) ?: return null
    val tradePrice = findTradePrice(
        aggressorSide = aggressor.side,
        aggressor = aggressor.key.price,
        passive = passive.key.price
    )

    return if (
        tradePrice == null ||
        cannotMatchTheseTwoClients(aggressor.client, passive.client)
    )
        findNextMatch(aggressor, passives, offset + 1)
    else if (priceHasCrossed(aggressor, passive))
        Match(passive, tradePrice)
    else null
}

private fun cannotMatchAnyFurther(aggressor: BookEntry, limitBook: LimitBook) =
    aggressor.size.availableSize <= 0 || limitBook.entries.isEmpty

private fun cannotMatchTheseTwoClients(aggressor: Client, passive: Client): Boolean =
    sameFirmAndSameFirmClient(aggressor, passive) || sameFirmButPossibleFirmAgainstClient(aggressor, passive)

private fun findPassive(passives: Seq<BookEntry>, offset: Int = 0): BookEntry? =
    if (offset < passives.size()) passives.get(offset) else null

data class Match(val passive: BookEntry, val tradePrice: Price)

data class MatchResult(val aggressor: BookEntry, val transaction: Transaction<BookId, Books>)