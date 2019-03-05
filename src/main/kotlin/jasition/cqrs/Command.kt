package jasition.cqrs

import arrow.core.Either

interface Command<K, A : Aggregate<K>>  {
    /**
     * Executes this command given the [aggregate]. The aggregate in domain has the final decision on how to respond to
     * the command and may perform differently than what the command specifies. Returns an [Exception] if the
     * command was rejected and no change was made to the [aggregate]. Returns an [Transaction] if the command was
     * executed.
     */
    fun execute(aggregate: A?) : Either<Exception, Transaction<K, A>>
}
