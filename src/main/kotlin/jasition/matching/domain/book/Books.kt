package jasition.matching.domain.book

import io.vavr.collection.TreeMap
import jasition.matching.domain.Aggregate
import jasition.matching.domain.order.Client
import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

data class Books(
    val id: String,
    val buyLimitBook: LimitBook = LimitBook(false),
    val sellLimitBook: LimitBook = LimitBook(true),
    val businessDate: LocalDateTime = LocalDateTime.now(),
    val tradingStatus: TradingStatus = TradingStatus.CLOSED,
    val lastSequenceNumber: Long = 0
) : Aggregate

data class LimitBook(val entries: TreeMap<BookEntryKey, BookEntry>) {
    constructor(ascending: Boolean) : this(TreeMap.empty(PriceTimeSequenceEntryComparator(ascending)))
}

data class BookEntryKey(
    val price: Long?,
    val whenSubmitted: Instant,
    val entryEventId: Long
)

class PriceTimeSequenceEntryComparator(ascending: Boolean) : Comparator<BookEntryKey> {
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
        return o1.entryEventId.compareTo(o2.entryEventId)
    }

    internal fun compare(p1: Long?, p2: Long?): Int {
        if (p1 != null && p2 != null) {
            return multiplier * p1.compareTo(p2)
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

data class BookEntry(
    val key: BookEntryKey,
    val clientEntryId: String,
    val client: Client,
    val orderType: OrderType,
    val side: Side,
    val availableSize: Int,
    val tradedSize: Int,
    val cancelledSize: Int,
    val timeInForce: TimeInForce
)

fun addBookEntry(
    books: Books,
    entry: BookEntry
): Books {

    var buyLimitBook = books.buyLimitBook;
    var sellLimitBook = books.sellLimitBook;

    if (Side.BUY.equals(entry.side)) {
        buyLimitBook = LimitBook(buyLimitBook.entries.put(entry.key, entry))
    } else {
        sellLimitBook = LimitBook(sellLimitBook.entries.put(entry.key, entry))
    }

    return Books(
        id = books.id,
        buyLimitBook = buyLimitBook,
        sellLimitBook = sellLimitBook,
        businessDate = books.businessDate,
        tradingStatus = books.tradingStatus,
        lastSequenceNumber = books.lastSequenceNumber
    )
}