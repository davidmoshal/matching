package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class EntryStatusTest : StringSpec({
    "Positive available sizes is partial fill"{
        EntryStatus.PARTIAL_FILL.traded(
            EntrySizes(
                available = 30,
                traded = 10,
                cancelled = 0
            )
        ) shouldBe EntryStatus.PARTIAL_FILL
    }
    "Zero available sizes is filled"{
        EntryStatus.PARTIAL_FILL.traded(
            EntrySizes(
                available = 0,
                traded = 10,
                cancelled = 0
            )
        ) shouldBe EntryStatus.FILLED
    }
})