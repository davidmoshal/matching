package jasition.matching

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import strikt.api.expectThat
import strikt.assertions.isNotBlank

internal object HelloWorldKtTest : Spek({
    given("Today is a good weather") {
        on("an occasion") {
            it("says hello world!") {
                expectThat(sayHello()).isNotBlank()
            }
        }
    }
})