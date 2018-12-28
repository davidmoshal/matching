package jasition.matching.domain.book.entry

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

internal class EntrySizesTest : StringSpec({
    "Moves available sizes to traded by trade sizes"{
        EntrySizes(
            available = 100, traded = 25, cancelled = 33
        ).traded(25) shouldBe EntrySizes(
            available = 75, traded = 50, cancelled = 33
        )
    }
    "Moves full available sizes to cancelled when cancelled"{
        EntrySizes(
            available = 100, traded = 25, cancelled = 33
        ).cancelled() shouldBe EntrySizes(
            available = 0, traded = 25, cancelled = 133
        )
    }
    "Adjusts available sizes when amended"{
        forall(
            row(100, 25, 33, 200, 142, 25, 33),
            row(100, 25, 33, 70, 12, 25, 33)
        ) { available, traded, cancelled, newOrderSize, newAvailable, newTraded, newCancelled ->
            EntrySizes(
                available = available, traded = traded, cancelled = cancelled
            ).amended(newOrderSize) shouldBe EntrySizes(
                available = newAvailable, traded = newTraded, cancelled = newCancelled
            )
        }
    }
    "Disallows amending down to equal to or lower than traded plus cancelled"{
        forall(
            row(100, 25, 33, 58),
            row(100, 25, 33, 57),
            row(100, 25, 33, 56)
        ) { available, traded, cancelled, newOrderSize ->
            shouldThrow<IllegalArgumentException> {
                EntrySizes(
                    available = available, traded = traded, cancelled = cancelled
                ).amended(newOrderSize)
            }
        }
    }
    "Disallows negative available, traded, and/or cancelled sizes(s)"{
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
                EntrySizes(
                    available = available, traded = traded, cancelled = cancelled
                )
            }
        }
    }
})