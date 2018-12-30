package jasition.cqrs

interface Aggregate<K> {
    fun aggregateId(): K
}

interface Command

interface Query

interface Report



