package jasition.matching.domain.quote

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class QuoteEntryTest : StringSpec({
    val quoteId = "quoteId"
    val quoteEntry = aQuoteEntry(quoteEntryId = randomId())
    val whoRequested = aFirmWithClient()
    val whenHappened = Instant.now()
    val eventId = anEventId()
    val timeInForce = TimeInForce.GOOD_TILL_CANCEL
    val bid = PriceWithSize(randomPrice(), randomSize())
    val offer = PriceWithSize(randomPrice(), randomSize())

    val expectedBuyEntry = BookEntry(
        price = bid.price,
        whoRequested = whoRequested,
        whenSubmitted = whenHappened,
        eventId = eventId,
        requestId = ClientRequestId(
            current = quoteEntry.quoteEntryId,
            collectionId = quoteEntry.quoteSetId,
            parentId = quoteId
        ),
        isQuote = true,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        timeInForce = timeInForce,
        sizes = EntrySizes(bid.size),
        status = EntryStatus.NEW
    )
    val expectedSellEntry = expectedBuyEntry.withKey(price = offer.price).copy(
        side = Side.SELL,
        sizes = EntrySizes(offer.size)
    )
    "Converts to ClientRequestId" {
        quoteEntry.toClientRequestId(quoteId = quoteId) shouldBe ClientRequestId(
            current = quoteEntry.quoteEntryId,
            collectionId = quoteEntry.quoteSetId,
            parentId = quoteId
        )
    }
    "Converts to an empty list of BookEntries if no bid nor offer sizes were found" {
        quoteEntry.copy(bid = null, offer = null)
            .toBookEntries(
                whoRequested = whoRequested,
                whenHappened = whenHappened,
                eventId = eventId,
                quoteId = quoteId,
                timeInForce = timeInForce
            ).size() shouldBe 0
    }

    "Converts to a list of one bid BookEntry if only bid size was found" {
        quoteEntry.copy(bid = bid)
            .toBookEntries(
                whoRequested = whoRequested,
                whenHappened = whenHappened,
                eventId = eventId,
                quoteId = quoteId,
                timeInForce = timeInForce
            ) shouldBe List.of(
            expectedBuyEntry
        )
    }
    "Converts to a list of one offer BookEntry if only offer size was found" {
        quoteEntry.copy(offer = offer)
            .toBookEntries(
                whoRequested = whoRequested,
                whenHappened = whenHappened,
                eventId = eventId,
                quoteId = quoteId,
                timeInForce = timeInForce
            ) shouldBe List.of(expectedSellEntry)
    }
    "Converts to a list of one bid and one offer BookEntry if both bid and offer sizes were found" {
        quoteEntry.copy(bid = bid, offer = offer)
            .toBookEntries(
                whoRequested = whoRequested,
                whenHappened = whenHappened,
                eventId = eventId,
                quoteId = quoteId,
                timeInForce = timeInForce
            ) shouldBe List.of(
            expectedBuyEntry,
            expectedSellEntry
        )
    }
})
