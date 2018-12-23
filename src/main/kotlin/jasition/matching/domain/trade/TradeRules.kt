package jasition.matching.domain.trade

import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Price

fun noFirmClientButOfSameFirm(aggressor: BookEntry, passive: BookEntry): Boolean =
    if (aggressor.client.firmClientId == null && passive.client.firmClientId == null)
        aggressor.client.firmId != passive.client.firmId
    else true

fun sameFirmClientAndSameFirm(aggressor: BookEntry, passive: BookEntry): Boolean =
    if (aggressor.client.firmClientId == null && passive.client.firmClientId == null)
        aggressor.client.firmId != passive.client.firmId
    else true

fun priceHasCrossed(aggressor: BookEntry, passive: BookEntry): Boolean {
    val aggressorPrice = aggressor.key.price
    val passivePrice = passive.key.price

    return if (aggressorPrice != null && passivePrice != null)
        aggressor.side.comparatorMultipler() * aggressorPrice.compareTo(passivePrice) <= 0
    else findTradePrice(aggressor, passive) != null
}

fun findTradePrice(aggressor: BookEntry, passive: BookEntry): Price? = passive.key.price ?: aggressor.key.price

fun getTradeSize(aggressor: BookEntry, passive: BookEntry) =
    Integer.min(aggressor.size.availableSize, passive.size.availableSize)

fun notAvailableForTrade(aggressor: BookEntry) = aggressor.size.availableSize <= 0