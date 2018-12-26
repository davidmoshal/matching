package jasition.matching.domain

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.*
import io.kotlintest.tables.row

class StringSpecDataDrivenTests : StringSpec({
    "maximum of two numbers" {
        forall(
            row(1, 5, 5),
            row(1, 0, 1),
            row(0, 0, 0)
        ) { a, b, max ->
            Math.max(a, b) shouldBe max
        }
    }
})
class StringTests : StringSpec({
    "strings.length should return size of string" {
        "hello".length shouldBe 5
    }
})

class FunTests : FunSpec({
    test("String length should return the length of the string") {
        "sammy".length shouldBe 5
        "".length shouldBe 0
    }
})

class ShouldTests : ShouldSpec({
    should("return the length of the string") {
        "sammy".length shouldBe 5
        "".length shouldBe 0
    }
})

class ShouldNestedTests : ShouldSpec({
    "String.length" {
        should("return the length of the string") {
            "sammy".length shouldBe 5
            "".length shouldBe 0
        }
    }
})

class WordTests : WordSpec({
    "String.length" should {
        "return the length of the string" {
            "sammy".length shouldBe 5
            "".length shouldBe 0
        }
    }
})

class FeatureTests : FeatureSpec({
    feature("the can of coke") {
        scenario("should be fizzy when I shake it") {
            // test here
        }
        scenario("and should be tasty") {
            // test heree
        }
    }
})

class BehaviourTests : BehaviorSpec({
    given("a broomstick") {
        `when`("I sit on it") {
            then("I should be able to fly") {
                // test code
            }
        }
        `when`("I throw it away") {
            then("it should come back") {
                // test code
            }
        }
    }
})

class FreeTests : FreeSpec({
    "String.length" - {
        "should return the length of the string" {
            "sammy".length shouldBe 5
            "".length shouldBe 0
        }
    }
})

class DescribeTests : DescribeSpec({
    describe("score") {
        it("start as zero") {
            // test here
        }
        context("with a strike") {
            it("adds ten") {
                // test here
            }
            it("carries strike to the next frame") {
                // test here
            }
        }
    }
})

class ExpectTests : ExpectSpec({
    context("a calculator") {
        expect("simple addition") {
            // test here
        }
        expect("integer overflow") {
            // test here
        }
    }
})