package jasition.matching.domain.book

import io.vavr.collection.Seq
import io.vavr.collection.TreeMap
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeSideEntry

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(side: Side) : this(
        TreeMap.empty(
            HighestBuyOrLowestSellPriceFirst(side)
                    then EarliestSubmittedTimeFirst
                    then SmallestEventIdFirst
        )
    )

    fun add(entry: BookEntry): LimitBook =
        copy(entries = entries.put(entry.key, entry))

    fun removeAll(toBeRemoved: Seq<BookEntry>): LimitBook =
        copy(entries = entries.removeAll(toBeRemoved.map { it.key }))

    fun update(entry: TradeSideEntry): LimitBook {
        val bookEntryKey = entry.toBookEntryKey()

        return copy(
            entries =
            if (entry.sizes.available <= 0)
                entries.remove(bookEntryKey)
            else
                entries.computeIfPresent(bookEntryKey) { _: BookEntryKey, existingEntry: BookEntry ->
                    existingEntry.copy(
                        sizes = entry.sizes,
                        status = entry.status
                    )
                }._2()
        )
    }
}
