package jasition.matching.domain.book

import io.vavr.collection.TreeMap
import jasition.matching.domain.EventId
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.BookEntryKey
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.order.event.TradeSideEntry
import java.util.Comparator

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(side: Side) : this(TreeMap.empty(PriceTimeSequenceEntryComparator(side)))

    fun add(entry: BookEntry): LimitBook =
        LimitBook(entries.put(entry.key, entry))

    fun update(entry: TradeSideEntry): LimitBook {
        val bookEntryKey = entry.toBookEntryKey()

        return LimitBook(
            if (entry.size.availableSize <= 0)
                entries.remove(bookEntryKey)
            else
                entries.put(
                    bookEntryKey, BookEntry(
                        key = bookEntryKey,
                        clientRequestId = entry.requestId,
                        client = entry.whoRequested,
                        entryType = entry.entryType,
                        side = entry.side,
                        timeInForce = entry.timeInForce,
                        size = entry.size,
                        status = entry.entryStatus
                    )
                )
        )
    }
}

class PriceTimeSequenceEntryComparator(val side: Side) :
    Comparator<BookEntryKey> {

    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int {
        val multiplier = side.comparatorMultipler()

        // First Priority : Price
        val samePrice = compare(o1.price, o2.price)
        if (samePrice != 0) {
            return multiplier * samePrice
        }

        // Second Priority : Time
        val sameTime = o1.whenSubmitted.compareTo(o2.whenSubmitted)
        if (sameTime != 0) {
            return multiplier * sameTime
        }

        // Last Priority : Event Sequence ID
        return multiplier * o1.eventId.value.compareTo(o2.eventId.value)
    }

    private fun compare(p1: Price?, p2: Price?): Int {
        if (p1 != null && p2 != null) {
            return p1.value.compareTo(p2.value)
        }

        // Null Price always come first no matter it is ascending or descending
        if (p1 == null) {
            return -1
        }

        if (p2 == null) {
            return 1
        }

        return 0
    }
}