@file:JvmName("PreconditionSetup")

package jasition.matching.domain

import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.command.PlaceMassQuoteCommand
import java.time.Instant

fun theBookId(): BookId = aBookId("bookId")

fun anEmptyBooks(): Books = aBooks(bookId = theBookId())

fun aBooksWithEntries(buyEntries: List<PriceWithSize>, sellEntries: List<PriceWithSize>): Books {
    var books = aBooks(bookId = theBookId())
    books = buyEntries.foldLeft(books) { b, p -> b.addBookEntry(aBookEntry(p, Side.BUY))}
    books = sellEntries.foldLeft(books) { b, p -> b.addBookEntry(aBookEntry(p, Side.SELL))}
    return books
}

fun aPriceWithSize(price : Long, size : Int) :PriceWithSize = PriceWithSize(Price(price), size)

fun aBookEntry(
    priceWithSize: PriceWithSize,
    side: Side, eventId: EventId = EventId(0)
): BookEntry = aBookEntry(
    requestId = aClientRequestId(randomId()),
    whoRequested = randomFirmWithClient(),
    price = priceWithSize.price,
    whenSubmitted = Instant.now(),
    eventId = eventId,
    entryType = EntryType.LIMIT,
    side = side,
    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
    sizes = EntrySizes(priceWithSize.size),
    status = EntryStatus.NEW
)

fun aPlaceOrderCommand(
    side: Side = Side.BUY,
    price: Long = 10,
    size: Int = 10
): PlaceOrderCommand =
    PlaceOrderCommand(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = theBookId(),
        entryType = EntryType.LIMIT,
        side = side,
        size = size,
        price = Price(price),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenRequested = Instant.now()
    )

fun aPlaceMassQuoteCommand(entries: List<QuoteEntry>): PlaceMassQuoteCommand =
    PlaceMassQuoteCommand(
        quoteId = "quoteId",
        whoRequested = aFirmWithoutClient(),
        bookId = theBookId(),
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = entries,
        whenRequested = Instant.now()
    )