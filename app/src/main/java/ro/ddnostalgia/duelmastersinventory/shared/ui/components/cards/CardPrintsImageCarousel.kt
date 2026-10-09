package ro.ddnostalgia.duelmastersinventory.shared.ui.components.cards

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.ddnostalgia.duelmastersinventory.shared.ui.components.core.ShadowedStarIcon

@Composable
fun CardPrintsImageCarousel(
    prints: List<CardPrint>,
    selectedPrint: CardPrint? = null,
    modifier:Modifier = Modifier,
    itemModifier: Modifier = Modifier
) {
    val viewModel: CardPrintsImageCarouselViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val flingBehavior: FlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val userData by viewModel.userData.collectAsState()

    LaunchedEffect(prints) {
        viewModel.setCardPrintIds(prints.map{ it.id })
    }

    LaunchedEffect(prints, selectedPrint) {
        val startIndex = selectedPrint?.let { prints.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        listState.scrollToItem(startIndex)
    }

    // Which print is currently centered/settled - drives the pill caption's active/dim tone
    // below (spec `2b`'s print-variant pill row), without touching the carousel's own
    // scroll/snap mechanics. Uses the item whose midpoint is nearest the viewport's midpoint
    // rather than firstVisibleItemIndex, which lags one item behind while an item is still
    // partially scrolled into view.
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }

    Box(modifier = modifier) {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            items(prints.size) { index ->
                val print = prints[index]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier=itemModifier) {
                        CardPrintImage(
                            cardPrint = print,
                            lowRes = false,
                        )
                        ShadowedStarIcon(
                            isSelected = userData[print.id]?.wishlist ?: false,
                            onClick = {
                                val data = userData[print.id] ?: CardPrintUserData(
                                    cardPrintId = print.id,
                                    wishlist = false
                                )
                                coroutineScope.launch {
                                    viewModel.updateUserData(
                                        data.copy(
                                            wishlist = !data.wishlist
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    val active = index == centerIndex
                    Text(
                        print.displayId(),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (active) 0.14f else 0.05f))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                    )
                }

            }
        }

        val firstVisible = listState.firstVisibleItemIndex
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        if (firstVisible > 0) {
            // Left Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val prevIndex = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(prevIndex)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color.White
                )
            }
        }

        if (lastVisible < prints.lastIndex) {
            // Right Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val nextIndex =
                            (listState.firstVisibleItemIndex + 1).coerceAtMost(prints.lastIndex)
                        listState.animateScrollToItem(nextIndex)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }
        }
    }
}