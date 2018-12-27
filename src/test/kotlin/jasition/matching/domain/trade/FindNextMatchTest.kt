package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.client.Client

internal class FindNextMatchTest : StringSpec({
    val entry = aBookEntry()
    "Not found when the offset is greater than the number of passive entries" {
        findNextMatch(aggressor = entry, passives = List.of(entry), offset = 2) shouldBe null
    }
    "Not found when the offset is equal to the number of passive entries" {
        findNextMatch(aggressor = entry, passives = List.of(entry), offset = 1) shouldBe null
    }
    val entryKey = entry.key
    val otherFirmClient = Client("otherFirm", "otherFirmClient")
    "Not found when the prices do not cross between aggressor and the last passive entry" {
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(9))
            ),
            passives = List.of(
                entry.copy(
                    side = Side.SELL,
                    key = entryKey.copy(price = Price(10)),
                    client = otherFirmClient
                ),
                entry.copy(
                    side = Side.SELL,
                    key = entryKey.copy(price = Price(11)),
                    client = otherFirmClient
                )

            )
        ) shouldBe null
    }
    "Found the first passive entry as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            client = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = List.of(
                passive,
                passive.copy(key = entryKey.copy(price = Price(9)))
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to same firm client and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            client = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = List.of(
                passive.copy(client = entry.client),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to same firm (firm against client) and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            client = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10))
            ),
            passives = List.of(
                passive.copy(client = entry.client.copy(firmClientId = null)),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to same firm (no client) and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            client = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price = Price(10)),
                client = entry.client.copy(firmClientId = null)
            ),
            passives = List.of(
                passive.copy(client = entry.client.copy(firmClientId = null)),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
    "Skipped the first passive entry due to no trade price found and found the second as the next matched" {
        val passive = entry.copy(
            side = Side.SELL,
            key = entryKey.copy(price = Price(10)),
            client = otherFirmClient
        )
        findNextMatch(
            aggressor = entry.copy(
                side = Side.BUY,
                key = entryKey.copy(price =null)
            ),
            passives = List.of(
                passive.copy(key = entryKey.copy(price = null)),
                passive
            )
        ) shouldBe Match(passive, Price(10))
    }
})