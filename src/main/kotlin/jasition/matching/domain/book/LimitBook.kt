package jasition.matching.domain.book

import io.vavr.collection.TreeMap
import jasition.matching.domain.order.Price
import java.util.Comparator

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(ascending: Boolean) : this(
        TreeMap.empty(
            PriceTimeSequenceEntryComparator(ascending)
        )
    )

    fun add(entry: BookEntry): LimitBook {
        return LimitBook(entries.put(entry.key, entry))
    }
}

class PriceTimeSequenceEntryComparator(ascending: Boolean) :
    Comparator<BookEntryKey> {
    val multiplier = if (ascending) 1 else -1

    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int {
        // First Priority : Price
        val samePrice = compare(o1.price, o2.price)
        if (samePrice != 0) {
            return samePrice
        }

        // Second Priority : Time
        val sameTime = o1.whenSubmitted.compareTo(o2.whenSubmitted)
        if (sameTime != 0) {
            return sameTime
        }

        // Last Priority : Event Sequence ID
        return o1.eventId.value.compareTo(o2.eventId.value)
    }

    internal fun compare(p1: Price?, p2: Price?): Int {
        if (p1 != null && p2 != null) {
            return multiplier * p1.value.compareTo(p2.value)
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