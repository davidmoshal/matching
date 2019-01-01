package jasition.matching.domain.quote.command

import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.PriceWithSize
import jasition.matching.domain.client.ClientRequestId

data class QuoteEntry(
    val quoteEntryId: String,
    val quoteSetId: String,
    val entryType: EntryType,
    val bid: PriceWithSize?,
    val offer: PriceWithSize?
) {

    fun toClientRequestId(quoteId: String): ClientRequestId = ClientRequestId(
        current = quoteEntryId, collectionId = quoteSetId, parentId = quoteId
    )
}

enum class QuoteModelType {
    QUOTE_ENTRY {
        override fun shouldCancelPreviousQuotes(): Boolean = true
    };

    abstract fun shouldCancelPreviousQuotes(): Boolean
}