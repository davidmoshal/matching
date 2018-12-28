package jasition.matching.domain.book

import io.vavr.collection.TreeMap
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeSideEntry

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(side: Side) : this(
        TreeMap.empty(
            HighestBuyOrLowestSellPriceFirst(side)
                    then EarliestSubmittedTimeFirst()
                    then SmallestEventIdFirst()
        )
    )

    fun add(entry: BookEntry): LimitBook =
        LimitBook(entries.put(entry.key, entry))

    fun update(entry: TradeSideEntry): LimitBook {
        val bookEntryKey = entry.toBookEntryKey()

        return LimitBook(
            if (entry.sizes.available <= 0)
                entries.remove(bookEntryKey)
            else
                entries.computeIfPresent(bookEntryKey) { existingKey: BookEntryKey, existingEntry: BookEntry ->
                    BookEntry(
                        key = existingKey,
                        requestId = existingEntry.requestId,
                        whoRequested = existingEntry.whoRequested,
                        entryType = existingEntry.entryType,
                        side = existingEntry.side,
                        timeInForce = existingEntry.timeInForce,
                        sizes = entry.sizes,
                        status = entry.status
                    )
                }._2()
        )
    }
}
