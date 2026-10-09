package ro.ddnostalgia.duelmastersinventory.shared.ui.components.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> LazyList(
    items: List<T>,
    key: ((T)->Any)?,
    modifier: Modifier = Modifier,
    gridView: Boolean = false,
    emptyTemplate: (@Composable (Modifier)->Unit)? = null,
    itemTemplate: @Composable (T, Modifier)->Unit,
) {
    if(items.isEmpty()) {
        if(emptyTemplate!=null) {
            emptyTemplate(Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    "No items.",
                    modifier=Modifier
                        .align(Alignment.Center)
                )
            }
        }

        return
    }

    if(!gridView) {
        LazyColumn(modifier) {
            this.items(items, key) { item ->
                itemTemplate(item, Modifier.padding(8.dp))
            }
        }
    }
    else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 75.dp) ,
            modifier = modifier,
        ) {
            items(items = items, key = key) { item ->
                itemTemplate(
                    item,
                    Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * A fixed-column-count grid where some rows are full-span headers, e.g. the
 * "DM-01 BASE SET ... 78/110" rows separating sets in the card grid. Callers
 * build one flat list interleaving header entries and item entries;
 * [isHeader] tells the grid which is which.
 */
@Composable
fun <T> LazyGridWithHeaders(
    rows: List<T>,
    columns: Int,
    key: (T) -> Any,
    isHeader: (T) -> Boolean,
    modifier: Modifier = Modifier,
    // Caller-hoisted so it can be created with rememberSaveable and survive navigating away and
    // back (e.g. cards list -> card view -> back) - a state built with the plain default here
    // would still be saveable in isolation, but hoisting makes the caller's intent to persist it
    // explicit rather than incidental.
    state: LazyGridState = rememberLazyGridState(),
    headerTemplate: @Composable (T) -> Unit,
    itemTemplate: @Composable (T, Modifier) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        modifier = modifier,
    ) {
        items(
            items = rows,
            key = key,
            span = { row -> if (isHeader(row)) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
        ) { row ->
            if (isHeader(row)) {
                Box(Modifier.fillMaxWidth()) { headerTemplate(row) }
            } else {
                itemTemplate(row, Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
fun <T, U> LazyList(
    items: List<T>,
    key: ((T)->Any)?,
    itemTransform: (T)->U,
    modifier: Modifier = Modifier,
    gridView: Boolean = false,
    itemTemplate: @Composable (U, Modifier)->Unit
) {
    if(!gridView) {
        LazyColumn(modifier) {
            this.items(items, key) { item ->
                itemTemplate(itemTransform(item), Modifier.padding(8.dp))
            }
        }
    }
    else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 75.dp) ,
            modifier = modifier,
        ) {
            items(items = items, key = key) { item ->
                itemTemplate(
                    itemTransform(item),
                    Modifier.padding(8.dp)
                )
            }
        }
    }
}