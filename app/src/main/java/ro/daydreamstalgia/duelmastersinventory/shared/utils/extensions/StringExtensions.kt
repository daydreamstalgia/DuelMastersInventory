package ro.daydreamstalgia.duelmastersinventory.shared.utils.extensions

fun String.safeSubstring(maxLength: Int): String =
    if (length <= maxLength) this else substring(0, maxLength)
