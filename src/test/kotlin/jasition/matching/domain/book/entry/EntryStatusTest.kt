package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class EntryStatusTest : StringSpec({
    "Positive available size is partial fill"{
        EntryStatus.PARTIAL_FILL.traded(
            EntryQuantity(
                availableSize = 30,
                tradedSize = 10,
                cancelledSize = 0
            )
        ) shouldBe EntryStatus.PARTIAL_FILL
    }
    "Zero available size is filled"{
        EntryStatus.PARTIAL_FILL.traded(
            EntryQuantity(
                availableSize = 0,
                tradedSize = 10,
                cancelledSize = 0
            )
        ) shouldBe EntryStatus.FILLED
    }
})