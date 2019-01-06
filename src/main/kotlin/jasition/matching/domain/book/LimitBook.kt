package jasition.matching.domain.book

import io.vavr.collection.Seq
import io.vavr.collection.TreeMap
import jasition.matching.domain.book.entry.*
import java.util.function.Function

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

    fun remove(bookEntryKey: BookEntryKey) : LimitBook {
        return copy(entries = entries.remove(bookEntryKey))
    }

    fun update(bookEntryKey: BookEntryKey, updateFunction: Function<BookEntry, BookEntry>): LimitBook {
        return copy(
            entries = entries.computeIfPresent(bookEntryKey) { _: BookEntryKey, existingEntry: BookEntry ->
                updateFunction.apply(existingEntry)
            }._2()
        )
    }
}
