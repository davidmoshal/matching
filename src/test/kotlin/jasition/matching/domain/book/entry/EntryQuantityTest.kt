package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

internal class EntryQuantityTest : StringSpec({
    "Moves available size to traded by trade size"{
        EntryQuantity(
            availableSize = 100, tradedSize = 25, cancelledSize = 33
        ).traded(25) shouldBe EntryQuantity(
            availableSize = 75, tradedSize = 50, cancelledSize = 33
        )
    }
    "Moves full available size to cancelled when cancelled"{
        EntryQuantity(
            availableSize = 100, tradedSize = 25, cancelledSize = 33
        ).cancelled() shouldBe EntryQuantity(
            availableSize = 0, tradedSize = 25, cancelledSize = 133
        )
    }
    "Reduces available size when amended down"{
        EntryQuantity(
            availableSize = 100, tradedSize = 25, cancelledSize = 33
        ).amended(70) shouldBe EntryQuantity(
            availableSize = 12, tradedSize = 25, cancelledSize = 33
        )
    }
    "Adds available size when amended up"{
        EntryQuantity(
            availableSize = 100, tradedSize = 25, cancelledSize = 33
        ).amended(200) shouldBe EntryQuantity(
            availableSize = 142, tradedSize = 25, cancelledSize = 33
        )
    }
    "Disallows amending down below traded plus cancelled"{
        shouldThrow<IllegalArgumentException> {
            EntryQuantity(
                availableSize = 100, tradedSize = 25, cancelledSize = 33
            ).amended(50)
        }
    }
    "Disallows negative available size"{
        shouldThrow<IllegalArgumentException> {
            EntryQuantity(
                availableSize = -100, tradedSize = 25, cancelledSize = 33
            )
        }
    }
    "Disallows negative traded size"{
        shouldThrow<IllegalArgumentException> {
            EntryQuantity(
                availableSize = 100, tradedSize = -25, cancelledSize = 33
            )
        }
    }
    "Disallows negative cancelled size"{
        shouldThrow<IllegalArgumentException> {
            EntryQuantity(
                availableSize = 100, tradedSize = 25, cancelledSize = -33
            )
        }
    }
})