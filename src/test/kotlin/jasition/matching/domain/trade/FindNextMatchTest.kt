package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.kotlin.list
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.anotherFirmWithClient
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side

internal class FindNextMatchTest : StringSpec({
    val entry = aBookEntry()
    "Not found when the offset is greater than the number of passive entries" {
        findNextMatch(aggressor = entry, passives = list(entry), offset = 2) shouldBe null
    }
    "Not found when the offset is equal to the number of passive entries" {
        findNextMatch(aggressor = entry, passives = list(entry), offset = 1) shouldBe null
    }
    val entryKey = entry.key
    val otherFirmClient = anotherFirmWithClient()
    "Not found when the prices do not cross between aggressor and the last passive entry" {
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(9))
            ),
            passives = list(
                entry.copy(
                    side = Side.SELL,
                    key = entryKey.copy(price = Price(10)),
                    whoRequested = otherFirmClient
                ),
                entry.copy(
                    side = Side.SELL,
                    key = entryKey.copy(price = Price(11)),
                    whoRequested = otherFirmClient
                )

            )
        ) shouldBe null
    }
    "Found the first passive entry as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            whoRequested = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = list(
                passive,
                passive.copy(key = entryKey.copy(price = Price(9)))
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to same firm whoRequested and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            whoRequested = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = list(
                passive.copy(whoRequested = entry.whoRequested),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to same firm (firm against whoRequested) and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            whoRequested = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = list(
                passive.copy(whoRequested = entry.whoRequested.copy(firmClientId = null)),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to both are quote and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            whoRequested = otherFirmClient,
            isQuote = false
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10)),
                whoRequested = entry.whoRequested,
                isQuote = true
            ),
            passives = list(
                passive.copy(isQuote = true),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to no trade price found and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            whoRequested = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = null)
            ),
            passives = list(
                passive.copy(key = entryKey.copy(price = null)),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
})