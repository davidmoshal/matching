package monad

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import jasition.monad.appendIfNotNullOrBlank
import jasition.monad.ifNotEqualsThenUse

internal class IfNotEqualsThenUseTest : StringSpec({
    "Uses left or right if they are equal" {
        ifNotEqualsThenUse(10, 10, 14) shouldBe 10
    }
    "Uses other if they are different" {
        ifNotEqualsThenUse(10, 11, 14) shouldBe 14
    }
})

internal class AppendIfNotNullOrBlankTest : StringSpec({
    forall(
        row("ABC", null, " : ", "ABC"),
        row("ABC", "", " : ", "ABC"),
        row("ABC", "\t", " : ", "ABC"),
        row(null, "123", " : ", "123"),
        row("", "123", " : ", "123"),
        row("\t", "123", " : ", "123"),
        row(null, null, " : ", null),
        row(null, "", " : ", null),
        row(null, "\t", " : ", null),
        row("", null, " : ", null),
        row("", "", " : ", null),
        row("", "\t", " : ", null),
        row("\t", null, " : ", null),
        row("\t", "", " : ", null),
        row("\t", "\t", " : ", null),
        row("ABC", "123", " : ", "ABC : 123")
    ) { left, right, delimiter, expectedResult ->
        "\"$left\" appends \"$right\" with delimiter \"$delimiter\" becomes \"$expectedResult\"" {
            appendIfNotNullOrBlank(left, right, delimiter) shouldBe expectedResult
        }
    }
})