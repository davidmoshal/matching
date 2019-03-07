package jasition.cqrs

import io.vavr.collection.List
import java.util.function.BiFunction

/**
 * [Validation] is the construct that facilitates validating a [Command] against an [Aggregate].
 */
interface Validation<KEY, AGG : Aggregate<KEY>, CMD : Command<KEY, AGG>, REJ : Event<KEY, AGG>> {
    fun validate(command: CMD, aggregate: AGG): REJ?
}

data class FailFast<KEY, AGG : Aggregate<KEY>, CMD : Command<KEY, AGG>, REJ : Event<KEY, AGG>>(
    val rules: List<Validation<KEY, AGG, CMD, REJ>>
) : Validation<KEY, AGG, CMD, REJ> {

    override fun validate(command: CMD, aggregate: AGG): REJ? = recurse(command, aggregate)

    private fun recurse(command: CMD, aggregate: AGG, offset: Int = 0): REJ? =
        if (offset >= rules.size())
            null
        else
            rules[offset].validate(command, aggregate) ?: recurse(command, aggregate, offset + 1)
}

data class CompleteValidation<KEY, AGG : Aggregate<KEY>, CMD : Command<KEY, AGG>, REJ : Event<KEY, AGG>>(
    val rules: List<Validation<KEY, AGG, CMD, REJ>>,
    val mergeFunction: BiFunction<REJ, REJ, REJ>
) : Validation<KEY, AGG, CMD, REJ> {

    override fun validate(command: CMD, aggregate: AGG): REJ? {
        with(rules.map { it.validate(command, aggregate) }.filterNotNull()) {
            return if (isEmpty()) null
            else reduce(mergeFunction::apply)
        }
    }
}
