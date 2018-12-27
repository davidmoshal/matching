package jasition.matching.domain.book

import io.kotlintest.specs.StringSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant


internal class BooksTest : StringSpec({
    val entry = BookEntry(
        price = Price(15),
        whenSubmitted = Instant.now(),
        eventId = EventId(1),
        clientRequestId = ClientRequestId("req1"),
        client = Client(firmId = "firm1", firmClientId = "client1"),
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        size = EntryQuantity(10),
        status = EntryStatus.NEW
    )
    "adding" {
        // TODO
    }
})