package jasition.matching.domain.book.entry

import io.vavr.kotlin.linkedHashMap
import io.vavr.kotlin.list
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.EntryType.MARKET
import jasition.matching.domain.book.entry.TimeInForce.*

object EntryTypeTimeInForceCombo {
    private val validCombos = linkedHashMap(
        Pair(LIMIT, list(GOOD_TILL_CANCEL, IMMEDIATE_OR_CANCEL, FILL_OR_KILL)),
        Pair(MARKET, list(IMMEDIATE_OR_CANCEL, FILL_OR_KILL))
    )

    fun isValid(entryType: EntryType, timeInForce: TimeInForce): Boolean =
        validCombos.get(entryType)
            .map { timeInForce in it }
            .getOrElse(false)
}