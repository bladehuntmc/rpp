package net.bladehunt.rpp.util.ignore

import kotlin.system.measureNanoTime

fun IgnorePattern(string: String): IgnorePattern = IgnorePattern.parse(string)

/**
 * Patterns should return the index after the last, even if it is a path char
 */
interface IgnorePattern {
    fun match(sequence: CharSequence, currentIndex: Int): Int

    companion object {
        /**
         * The pattern failed at that step, possible false state
         */
        const val FAIL = -1

        const val SEPARATOR = '/'

        const val SEPARATOR_STRING = SEPARATOR.toString()

        fun parse(sequence: CharSequence): IgnorePattern {
            if (sequence.isEmpty()) return Continue

            return when {
                // The recursive pattern takes priority over path separators
                sequence.contains("**") -> {
                    val (first, second) = sequence.split("**", limit = 2)

                    val left =
                        if (first.isNotEmpty()) parse(first.removeSuffix(SEPARATOR_STRING))
                        else Continue

                    val right =
                        if (second.isNotEmpty()) parse(second.removePrefix(SEPARATOR_STRING))
                        else Continue

                    Recursive(left, right)
                }
                sequence.contains('*') -> {
                    val (first, second) = sequence.split('*', limit = 2)

                    val left = parse(first)

                    val right = parse(second)

                    Star(left, right, if (right == Continue) Star.MatchType.FULL else Star.MatchType.PART)
                }
                sequence.contains('?') -> {
                    val (first, second) = sequence.split('?', limit = 2)
                    val left = parse(first)
                    val right = parse(second)

                    when {
                        left !is Continue && right !is Continue -> Composite(
                            Composite(parse(first), SingleChar),
                            parse(second)
                        )
                        left !is Continue -> Composite(parse(first), SingleChar)
                        right !is Continue -> Composite(SingleChar, parse(second))
                        else -> SingleChar
                    }
                }
                sequence.contains(SEPARATOR) -> {
                    val (first, second) = sequence.split(SEPARATOR, limit = 2)
                    val left = parse(first)
                    val right = parse(second)

                    when {
                        left !is Continue && right !is Continue -> Composite(
                            Composite(parse(first), Separator),
                            parse(second)
                        )
                        left !is Continue -> Composite(parse(first), Separator)
                        right !is Continue -> Composite(Separator, parse(second))
                        else -> Separator
                    }
                }
                else -> Literal(sequence)
            }
        }
    }

    data object Continue : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int = currentIndex
    }

    data object NextSeparator : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            for (i in currentIndex until sequence.length) {
                if (sequence[i] == SEPARATOR) return i + 1
            }

            return FAIL
        }
    }

    data object NextSeparatorOrEnd : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            for (i in currentIndex until sequence.length) {
                if (sequence[i] == SEPARATOR) return i
            }

            return sequence.length - 1
        }
    }

    data object Separator : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            return if (sequence.getOrNull(currentIndex) == SEPARATOR) currentIndex + 1 else FAIL
        }
    }

    data object SeparatorOrCurrent : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            return if (sequence.getOrNull(currentIndex) == SEPARATOR) currentIndex + 1 else currentIndex
        }
    }

    data object End : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int =
            if (currentIndex == sequence.length) currentIndex else FAIL
    }

    data object SingleChar : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            if (sequence[currentIndex] == SEPARATOR) return FAIL

            return currentIndex + 1
        }
    }

    data class Literal(val sequence: CharSequence) : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            for (i in 0 until this.sequence.length) {
                if (sequence.getOrNull(i + currentIndex) != this.sequence[i]) return FAIL
            }

            return currentIndex + this.sequence.length
        }
    }

    data class Star(val left: IgnorePattern, val right: IgnorePattern, val matchType: MatchType) : IgnorePattern {
        enum class MatchType {
            FULL,
            PART
        }

        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            var result = left.match(sequence, currentIndex)
            if (result == FAIL) return FAIL

            result = SeparatorOrCurrent.match(sequence, result)

            while (result < sequence.length) {
                val char = sequence[result]

                result++

                when (matchType) {
                    MatchType.FULL -> if (char == SEPARATOR) break
                    MatchType.PART -> {
                        val rightResult = right.match(sequence, SeparatorOrCurrent.match(sequence, result))
                        if (rightResult == FAIL) continue
                        return rightResult
                    }
                }
            }

            return right.match(sequence, SeparatorOrCurrent.match(sequence, result))
        }
    }

    data class Recursive(val left: IgnorePattern, val right: IgnorePattern) : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int {
            var result = left.match(sequence, currentIndex)
            if (result == FAIL) return FAIL

            val currentResult = right.match(sequence, SeparatorOrCurrent.match(sequence, result))

            if (currentResult != FAIL) return currentResult

            while (result < sequence.length) {
                val char = sequence[result]

                result++

                if (char == SEPARATOR) {
                    val rightResult = right.match(sequence, result)

                    if (rightResult == FAIL) continue

                    return rightResult
                }
            }

            return right.match(sequence, SeparatorOrCurrent.match(sequence, result - 1))
        }
    }

    data class Composite(
        val left: IgnorePattern,
        val right: IgnorePattern
    ) : IgnorePattern {
        override fun match(sequence: CharSequence, currentIndex: Int): Int =
            right.match(sequence, left.match(sequence, currentIndex).also { if (it == FAIL) return it })
    }
}