package jasition.matching.domain.book

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.randomPrice
import java.time.Instant
import java.util.function.Function

internal class LimitBookTest : StringSpec({
    "Prioritises BUY entry of higher prices over lower prices"{
        val lowerPrice = aBookEntry(side = Side.BUY, price = Price(10))
        val higherPrice = aBookEntry(side = Side.BUY, price = Price(11))

        val newBook = LimitBook(Side.BUY).add(lowerPrice).add(higherPrice)

        newBook.entries.values() shouldBe List.of(higherPrice, lowerPrice)
    }
    "Prioritises BUY entry of earlier submission over later given the same price"{
        val now = Instant.now()
        val later = aBookEntry(side = Side.BUY, whenSubmitted = now)
        val earlier = aBookEntry(side = Side.BUY, whenSubmitted = now.minusMillis(1))

        val newBook = LimitBook(Side.BUY).add(later).add(earlier)

        newBook.entries.values() shouldBe List.of(earlier, later)
    }
    "Prioritises BUY entry of smaller Event ID over bigger given the same price and same submission time"{
        val now = Instant.now()
        val bigger = aBookEntry(side = Side.BUY, eventId = EventId(2), whenSubmitted = now)
        val smaller = aBookEntry(side = Side.BUY, eventId = EventId(1), whenSubmitted = now)

        val newBook = LimitBook(Side.BUY).add(bigger).add(smaller)

        newBook.entries.values() shouldBe List.of(smaller, bigger)
    }
    "Replaces BUY entry of same price, same submission time, and same Event ID"{
        val now = Instant.now()
        val entry1 = aBookEntry(side = Side.BUY, whenSubmitted = now, status = EntryStatus.NEW)
        val entry2 = aBookEntry(side = Side.BUY, whenSubmitted = now, status = EntryStatus.PARTIAL_FILL)

        val newBook = LimitBook(Side.BUY).add(entry1).add(entry2)

        newBook.entries.values() shouldBe List.of(entry2)
    }
    "Prioritises SELL entry of lower prices over higher prices"{
        val lowerPrice = aBookEntry(side = Side.SELL, price = Price(10))
        val higherPrice = aBookEntry(side = Side.SELL, price = Price(11))

        val newBook = LimitBook(Side.SELL).add(lowerPrice).add(higherPrice)

        newBook.entries.values() shouldBe List.of(lowerPrice, higherPrice)
    }
    "Prioritises SELL entry of earlier submission over later given the same price"{
        val now = Instant.now()
        val later = aBookEntry(side = Side.SELL, whenSubmitted = now)
        val earlier = aBookEntry(side = Side.SELL, whenSubmitted = now.minusMillis(1))

        val newBook = LimitBook(Side.SELL).add(later).add(earlier)

        newBook.entries.values() shouldBe List.of(earlier, later)
    }
    "Prioritises SELL entry of smaller Event ID over bigger given the same price and same submission time"{
        val now = Instant.now()
        val bigger = aBookEntry(side = Side.SELL, eventId = EventId(2), whenSubmitted = now)
        val smaller = aBookEntry(side = Side.SELL, eventId = EventId(1), whenSubmitted = now)

        val newBook = LimitBook(Side.SELL).add(bigger).add(smaller)

        newBook.entries.values() shouldBe List.of(smaller, bigger)
    }
    "Replaces SELL entry of same price, same submission time, and same Event ID"{
        val now = Instant.now()
        val entry1 = aBookEntry(side = Side.SELL, whenSubmitted = now, status = EntryStatus.NEW)
        val entry2 = aBookEntry(side = Side.SELL, whenSubmitted = now, status = EntryStatus.PARTIAL_FILL)

        val newBook = LimitBook(Side.SELL).add(entry1).add(entry2)

        newBook.entries.values() shouldBe List.of(entry2)
    }
    "Updates an entry from the book by given key"{
        val entry1 = aBookEntry(
            side = Side.SELL,
            price = Price(10),
            sizes = EntrySizes(available = 0, traded = 15, cancelled = 0),
            status = EntryStatus.FILLED
        )
        val entry2 = entry1.copy(status = EntryStatus.CANCELLED)
        val newBook = LimitBook(Side.SELL)
            .add(entry1)
            .update(entry1.key, Function { it.copy(status = EntryStatus.CANCELLED)})

        newBook.entries.values() shouldBe List.of(entry2)
    }
    "Removes an entry from the book by given key"{
        val entry1 = aBookEntry(
            side = Side.SELL,
            price = Price(10),
            sizes = EntrySizes(available = 0, traded = 15, cancelled = 0)
        )
        val entry2 = entry1.withKey(price = randomPrice())
        val newBook = LimitBook(Side.SELL)
            .add(entry1)
            .add(entry2)
            .remove(entry1.key)

        newBook.entries.values() shouldBe List.of(entry2)
    }
    "Removes all entries from the book"{
        val entry1 = aBookEntry(
            side = Side.SELL,
            price = Price(10),
            sizes = EntrySizes(available = 0, traded = 15, cancelled = 0)
        )
        val entry2 = entry1.withKey(price = randomPrice())
        val newBook = LimitBook(Side.SELL)
            .add(entry1)
            .add(entry2)
            .removeAll(List.of(entry1, entry2))

        newBook.entries.size() shouldBe 0
    }
})