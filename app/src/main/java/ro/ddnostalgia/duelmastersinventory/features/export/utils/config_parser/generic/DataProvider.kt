package ro.ddnostalgia.duelmastersinventory.features.export.utils.config_parser.generic

import android.util.Log
import ro.ddnostalgia.duelmastersinventory.features.export.model.SheetRow
import ro.ddnostalgia.duelmastersinventory.features.export.model.SheetTable

open class DataProvider<T>(
    private val parser: ParserConfig<T>,
    private val getAll: ()->Sequence<T>
) {
    fun generateSheets(
        columns: List<String>,
        filtersStr: Map<String, String>,
        groupsStr: List<Pair<String, String>>

    ): Sequence<SheetTable> {
        Log.d("DataProvider", "generateSheets($columns, $filtersStr, $groupsStr)")

        return sequence {
            val filter = filtersStr.map {
                parser.resolveFilter(it.key, it.value)
            }.fold({ _: T -> true }) { acc, f -> { t -> acc(t) && f(t) } }

            val groups = groupsStr
                .map {
                    it.first to parser.resolveGroupBelonging(it.second)
                }.ifEmpty {
                    listOf("items" to { _: T -> true })
                }

            for ((name, belongs) in groups) {
                yield(SheetTable(
                    title = name,
                    header = columns,
                    rows = sequence {
                        for (item in getAll().filter(filter)) {
                            if (belongs(item)) {
                                val cells = columns.map {
                                    parser.getCellValue(item, it)
                                }
                                yield(SheetRow(
                                    cells = cells,
                                    background = parser.getRowBackground?.invoke(item)
                                ))
                            }
                        }
                    }
                ))
            }
        }
    }
}