package jasition.matching.domain.trade

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

fun matchAndPlaceEntry(
    bookEntry: BookEntry,
    books: Books
): Transaction<BookId, Books> {
    val (aggressor, transaction) = match(
        aggressor = bookEntry,
        books = books
    )

    return if (aggressor.timeInForce.canStayOnBook(aggressor.sizes))
        transaction.thenPlay(
            aggressor.toEntryAddedToBookEvent(
                eventId = transaction.aggregate.lastEventId.next(),
                bookId = books.bookId
            )
        )
    else transaction
}

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

    val tradeSize = getTradeSize(aggressor.sizes, nextMatch.passive.sizes)
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
    return findPassive(passives, offset)?.let { passive ->
        if (cannotMatchTheseTwoPrices(aggressor.key.price, passive.key.price)
            || cannotMatchTheseTwoClients(aggressor.whoRequested, passive.whoRequested)
            || cannotMatchTheseTwoEntries(aggressor.isQuote, passive.isQuote)
        ) return findNextMatch(
            aggressor = aggressor,
            passives = passives,
            offset = offset + 1
        )

        findTradePrice(
            aggressorSide = aggressor.side,
            aggressor = aggressor.key.price,
            passive = passive.key.price
        )?.let { tradePrice ->
            return Match(passive, tradePrice)
        }
    }
}

private fun cannotMatchAnyFurther(aggressor: BookEntry, limitBook: LimitBook) =
    aggressor.sizes.available <= 0 || limitBook.entries.isEmpty

private fun cannotMatchTheseTwoClients(aggressor: Client, passive: Client): Boolean =
    sameFirmAndSameFirmClient(aggressor, passive) || sameFirmButPossibleFirmAgainstClient(aggressor, passive)

private fun cannotMatchTheseTwoEntries(aggressorIsQuote: Boolean, passiveIsQuote: Boolean): Boolean =
    aggressorIsQuote && passiveIsQuote

private fun cannotMatchTheseTwoPrices(aggressor: Price?, passive: Price?): Boolean =
    aggressor == null && passive == null

private fun findPassive(passives: Seq<BookEntry>, offset: Int): BookEntry? =
    if (offset < passives.size()) passives.get(offset) else null

data class Match(val passive: BookEntry, val tradePrice: Price)

data class MatchResult(val aggressor: BookEntry, val transaction: Transaction<BookId, Books>)