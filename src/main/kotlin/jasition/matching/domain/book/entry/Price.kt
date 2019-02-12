package jasition.matching.domain.book.entry

/**
 * The data class that models price. Long is used as we assume the value is normalised with a decimal place
 * or precision value constant in the the instrument. E.g. 1234 is used to represent 12.34 while 2 as decimal
 * places is the constant for the given instrument.
 */
data class Price(val value: Long) : Comparable<Price> {
    override fun compareTo(other: Price): Int = value.compareTo(other.value)
}

class PriceComparator(private val multiplier: Int = 1) : Comparator<Price> {
    override fun compare(o1: Price, o2: Price): Int = multiplier * o1.value.compareTo(o2.value)
}

/**
 * TODO : replace it by SizeAndPrice
 */
data class PriceWithSize(val price : Price, val size : Int)