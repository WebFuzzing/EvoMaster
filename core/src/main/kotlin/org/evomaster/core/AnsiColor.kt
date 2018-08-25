package org.evomaster.core

/**
 * Use special ANSI escape codes to have colors on the terminal.
 * See:
 * https://stackoverflow.com/questions/5947742/how-to-change-the-output-color-of-echo-in-linux
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * Created by arcuri82 on 24-Aug-18.
 */
enum class AnsiColor(val code: String) {

    NONE("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m");

    companion object {

        fun colored(s: String, color: AnsiColor): String {
            return "${color.code}$s${NONE.code}"
        }

        fun inBlue(s: String) = colored(s, BLUE)

        fun inRed(s: String) = colored(s, RED)

        fun inYellow(s: String) = colored(s, YELLOW)

        fun inGreen(s: String) = colored(s, GREEN)
    }
}