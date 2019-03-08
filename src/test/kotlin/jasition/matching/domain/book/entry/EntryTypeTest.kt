package jasition.matching.domain.book.entry

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.EntryType.MARKET

internal class EntryTypeTest : StringSpec({
    forall(
        row(LIMIT, true),
        row(MARKET, false)
    ) { entryType, expectedResult ->
        "Entry Type $entryType enforces that price must be ${if (expectedResult) "pre" else "ab" }sent" {
            entryType.isPriceRequiredOrMustBeNull() shouldBe expectedResult
        }
    }
})