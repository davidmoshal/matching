package jasition.matching.domain.trade

import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.client.Client

fun sameFirmButPossibleFirmAgainstClient(client: Client, other: Client): Boolean =
    client.firmId == other.firmId
            && (client.firmClientId == null || other.firmClientId == null)

fun sameFirmAndSameFirmClient(client: Client, other: Client): Boolean =
    client == other

fun findTradePrice(aggressorSide: Side, aggressor: Price?, passive: Price?): Price? =
    if (aggressor != null && passive != null)
        if (aggressorSide.comparatorMultiplier() * aggressor.compareTo(passive) <= 0) passive
        else null
    else passive ?: aggressor

fun getTradeSize(aggressor: EntrySizes, passive: EntrySizes): Int =
    Integer.min(aggressor.available, passive.available)

