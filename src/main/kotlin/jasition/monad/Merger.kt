package jasition.monad

fun <T> ifNotEqualsThenUse(left: T, right: T, other: T): T = if (left == right) right else other

fun appendIfNotNullOrBlank(left: String?, right: String?, delimiter: String): String? =
    listOfNotNull(left, right)
        .filter(String::isNotBlank)
        .joinToString(delimiter)
        .takeUnless(String::isNullOrBlank)