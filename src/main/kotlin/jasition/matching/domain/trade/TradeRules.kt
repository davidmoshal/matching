package jasition.matching.domain.trade

import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.Price

fun sameFirmButPossibleFirmAgainstClient(aggressor: BookEntry, passive: BookEntry): Boolean =
    aggressor.client.firmId == passive.client.firmId
            && (aggressor.client.firmClientId == null || passive.client.firmClientId == null)

fun sameFirmAndSameFirmClient(aggressor: BookEntry, passive: BookEntry): Boolean =
    aggressor.client == passive.client

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