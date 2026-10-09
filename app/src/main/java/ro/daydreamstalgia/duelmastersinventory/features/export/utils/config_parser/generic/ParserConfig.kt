package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic

import androidx.compose.ui.graphics.Color
import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetCell

open class ParserConfig<T>(
    val resolveFilter: (name:String, value:String)->((T)->Boolean),
    val resolveGroupBelonging: (String)->((T)->Boolean),
    val getCellValue: (T, column:String)->SheetCell,
    val getRowBackground: ((T)->Color)?=null,
) {
    companion object {
        fun parseSimpleList(input: String): List<String> {
            val trimmed = input.removePrefix("[").removeSuffix("]").trim()
            if (trimmed.isEmpty()) return emptyList()
            return trimmed.split(",").map { it.trim() }
        }

        fun parseFunction(pattern: String): Pair<String, List<String>>? {
            // Matches "fun_name(arg0,arg1,...,argn)"
            val regex = Regex("""^([a-zA-Z_][a-zA-Z0-9_]*)\((.*)\)$""")
            val match = regex.matchEntire(pattern) ?: return null

            val functionName = match.groupValues[1]
            val argsString = match.groupValues[2].trim()

            // Handle empty argument list
            val args = if (argsString.isEmpty()) {
                emptyList()
            } else {
                // Split by comma, trim whitespace
                argsString.split(",").map { it.trim() }
            }

            return functionName to args
        }
    }
}

