package jasition.matching.domain.book

import io.vavr.collection.TreeMap
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.BookEntryKey
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.entryComparator
import jasition.matching.domain.trade.event.TradeSideEntry

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(side: Side) : this(TreeMap.empty(entryComparator(side)))

    fun add(entry: BookEntry): LimitBook =
        LimitBook(entries.put(entry.key, entry))

    fun update(entry: TradeSideEntry): LimitBook {
        val bookEntryKey = entry.toBookEntryKey()

        return LimitBook(
            if (entry.size.availableSize <= 0)
                entries.remove(bookEntryKey)
            else
                entries.computeIfPresent(bookEntryKey, { existingKey: BookEntryKey, existingEntry: BookEntry ->
                    BookEntry(
                        key = existingKey,
                        clientRequestId = existingEntry.clientRequestId,
                        client = existingEntry.client,
                        entryType = existingEntry.entryType,
                        side = existingEntry.side,
                        timeInForce = existingEntry.timeInForce,
                        size = entry.size,
                        status = entry.entryStatus
                    )
                })._2()
        )
    }
}