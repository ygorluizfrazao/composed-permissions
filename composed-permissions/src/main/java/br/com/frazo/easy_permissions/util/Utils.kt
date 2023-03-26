package br.com.frazo.easy_permissions.util

fun String.capitalizeWords(): String {
    val trimmed = trim()
    if(trimmed.isBlank())
        return this
    return trimmed.lowercase().mapIndexed { index, char ->
        if (index == 0)
            return@mapIndexed char.uppercase()
        if (this[index - 1].toString().isBlank()) {
            return@mapIndexed char.uppercase()
        }
        char.toString()
    }.reduce{ acc, s->
        acc+s
    }
}