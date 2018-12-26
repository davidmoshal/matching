package jasition.matching.domain.book.entry

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

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
    "Adjusts available size when amended"{
        forall(
            row(100, 25, 33, 200, 142, 25, 33),
            row(100, 25, 33, 70, 12, 25, 33)
        ) { available, traded, cancelled, newOrderSize, newAvailable, newTraded, newCancelled ->
            EntryQuantity(
                availableSize = available, tradedSize = traded, cancelledSize = cancelled
            ).amended(newOrderSize) shouldBe EntryQuantity(
                availableSize = newAvailable, tradedSize = newTraded, cancelledSize = newCancelled
            )
        }
    }
    "Disallows amending down to equal to or lower than traded plus cancelled"{
        forall(
            row(100, 25, 33, 58),
            row(100, 25, 33, 57)
        ) { available, traded, cancelled, newOrderSize ->
            shouldThrow<IllegalArgumentException> {
                EntryQuantity(
                    availableSize = available, tradedSize = traded, cancelledSize = cancelled
                ).amended(newOrderSize)
            }
        }
    }
    "Disallows negative available, traded, and/or cancelled size(s)"{
        forall(
            row(-100, 25, 33),
            row(100, -25, 33),
            row(100, 25, -33),
            row(-100, -25, 33),
            row(100, -25, -33),
            row(-100, 25, -33),
            row(-100, -25, -33)
        ) { available, traded, cancelled ->
            shouldThrow<IllegalArgumentException> {
                EntryQuantity(
                    availableSize = available, tradedSize = traded, cancelledSize = cancelled
                )
            }
        }
    }
})