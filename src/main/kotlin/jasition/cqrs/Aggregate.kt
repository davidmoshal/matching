package jasition.cqrs

interface Aggregate<K> {
    fun aggregateId(): K
}