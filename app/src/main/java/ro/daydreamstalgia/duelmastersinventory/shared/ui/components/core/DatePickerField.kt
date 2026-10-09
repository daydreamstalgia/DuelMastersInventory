package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatePickerField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date",
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    val inputDate = date ?: LocalDate.now()

    // State for month & year (start from the current date)
    var month by remember(inputDate) { mutableStateOf(inputDate.monthValue - 1) } // Calendar.MONTH is 0-based
    var year by remember(inputDate) { mutableStateOf(inputDate.year) }

    val daysInMonth = remember(month, year) {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based index
        val list = mutableListOf<Int?>()
        repeat(startDayOfWeek) { list.add(null) }
        for (d in 1..maxDay) list.add(d)

        // pad last row to full weeks
        val remainder = list.size % 7
        if (remainder != 0) {
            repeat(7 - remainder) { list.add(null) }
        }
        list
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        TextField(
            value = inputDate.toString(), // LocalDate ISO-8601 default format: yyyy-MM-dd
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize()
        ) {
            Column(Modifier.padding(8.dp)) {
                // Header with month navigation
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (month == 0) {
                            month = 11
                            year--
                        } else month--
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev")
                    }

                    Text(
                        text = "${DateFormatSymbols().months[month]} $year",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = {
                        if (month == 11) {
                            month = 0
                            year++
                        } else month++
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                    }
                }

                // Days of week
                Row(Modifier.fillMaxWidth()) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Calendar grid
                FlowRow(
                    maxItemsInEachRow = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6 * 48.dp)
                ) {
                    daysInMonth.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (day != null &&
                                        day == inputDate.dayOfMonth &&
                                        month + 1 == inputDate.monthValue &&
                                        year == inputDate.year
                                    ) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    else Color.Transparent
                                )
                                .clickable(enabled = day != null) {
                                    if (day != null) {
                                        val selected = LocalDate.of(year, month + 1, day)
                                        onDateChange(selected)
                                        expanded = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day?.toString() ?: "")
                        }
                    }
                }
            }
        }
    }
}
