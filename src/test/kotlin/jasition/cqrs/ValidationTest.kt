package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.*
import io.vavr.collection.List
import java.util.function.BiFunction

internal class ValidationTest : StringSpec({
    val passedValidation1 = mockk<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>(relaxed = true)
    val passedValidation2 = mockk<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>(relaxed = true)
    val failedValidation1 = mockk<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>(relaxed = true)
    val failedValidation2 = mockk<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>(relaxed = true)

    val aggregate = TestAggregate()
    val command = TestCommand()

    val reason1 = "Bad weather"
    val reason2 = "Jam traffic"

    val testRejectedEvent1 = TestRejectedEvent(
        aggregateId = aggregate.aggregateId,
        eventId = EventId(1),
        reason = reason1
    )
    val testRejectedEvent2 = testRejectedEvent1.copy(
        reason = reason2
    )

    val mergeFunction = BiFunction<TestRejectedEvent, TestRejectedEvent, TestRejectedEvent> { r1, r2 ->
        TestRejectedEvent(
            aggregateId = r1.aggregateId,
            eventId = r1.eventId,
            reason = "${r1.reason}; ${r2.reason}"
        )
    }

    every { passedValidation1.validate(command, aggregate) } returns null
    every { passedValidation2.validate(command, aggregate) } returns null
    every { failedValidation1.validate(command, aggregate) } returns testRejectedEvent1
    every { failedValidation2.validate(command, aggregate) } returns testRejectedEvent2

    "FailFast passes if the list of validations is empty" {
        FailFast(List.empty<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>()).validate(
            command,
            aggregate
        ) shouldBe null
    }
    "FailFast passes if all validations passed" {
        FailFast(List.of(passedValidation1, passedValidation2)).validate(command, aggregate) shouldBe null

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2)
    }
    "FailFast fails if it encountered a failed validation" {
        FailFast(List.of(passedValidation1, passedValidation2, failedValidation1)).validate(
            command,
            aggregate
        ) shouldBe testRejectedEvent1

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        verify { failedValidation1.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2)
    }
    "FailFast fails if it encountered the first failed validation and the rest of validations not called" {
        FailFast(List.of(passedValidation1, passedValidation2, failedValidation1, failedValidation2)).validate(
            command,
            aggregate
        ) shouldBe testRejectedEvent1

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        verify { failedValidation1.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2, failedValidation1)
        verify { failedValidation2 wasNot Called }
    }
    "CompleteValidation passes if the list of validations is empty" {
        CompleteValidation(
            List.empty<Validation<Int, TestAggregate, TestCommand, TestRejectedEvent>>(),
            mergeFunction
        ).validate(
            command,
            aggregate
        ) shouldBe null
    }
    "CompleteValidation passes if all validations passed" {
        CompleteValidation(List.of(passedValidation1, passedValidation2), mergeFunction).validate(
            command,
            aggregate
        ) shouldBe null

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2)
    }
    "CompleteValidation fails if it encountered a validation" {
        CompleteValidation(List.of(passedValidation1, passedValidation2, failedValidation1), mergeFunction).validate(
            command,
            aggregate
        ) shouldBe testRejectedEvent1

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        verify { failedValidation1.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2, failedValidation1)
    }
    "CompleteValidation fails if it encountered at least one validation and the rest of failed validations are merged" {
        CompleteValidation(
            List.of(
                passedValidation1,
                passedValidation2,
                failedValidation1,
                failedValidation2
            ), mergeFunction
        ).validate(
            command,
            aggregate
        ) shouldBe TestRejectedEvent(
            aggregateId = aggregate.aggregateId,
            eventId = EventId(1),
            reason = "$reason1; $reason2"
        )

        verify { passedValidation1.validate(command, aggregate) }
        verify { passedValidation2.validate(command, aggregate) }
        verify { failedValidation1.validate(command, aggregate) }
        verify { failedValidation2.validate(command, aggregate) }
        confirmVerified(passedValidation1, passedValidation2, failedValidation1, failedValidation2)
    }
})