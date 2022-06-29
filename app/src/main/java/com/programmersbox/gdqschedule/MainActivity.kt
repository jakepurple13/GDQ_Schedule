package com.programmersbox.gdqschedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.gdqschedule.ui.theme.GDQScheduleTheme
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GDQScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GDQSchedule()
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPagerApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun GDQSchedule(viewModel: GameViewModel = viewModel()) {

    val currentTime by currentTime()

    val topBarState = rememberTopAppBarScrollState()
    val topBarBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    Scaffold(
        modifier = Modifier.nestedScroll(topBarBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = { Text("GDQ Schedule") },
                scrollBehavior = topBarBehavior
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val pagerState = rememberPagerState()
            val scope = rememberCoroutineScope()
            val days = viewModel.gameInfoGrouped
            if (days.isNotEmpty()) {

                LaunchedEffect(Unit) {
                    val c = days.keys.indexOf(viewModel.dateFormat.format(currentTime))
                    if (c != -1) pagerState.animateScrollToPage(c)
                }

                ScrollableTabRow(
                    containerColor = TopAppBarDefaults.smallTopAppBarColors()
                        .containerColor(scrollFraction = topBarBehavior.scrollFraction).value,
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    days.keys.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }

                HorizontalPager(
                    count = days.keys.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(top = 2.dp)
                    ) {
                        val list = days.values.toList()[page]
                        itemsIndexed(list) { i, it ->
                            val d = Date(currentTime)
                            val isCurrentGame =
                                d.after(it.startTimeAsDate) && d.before(list.getOrNull(i + 1)?.startTimeAsDate)
                            val cardColor =
                                if (isCurrentGame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(animateColorAsState(cardColor).value)
                            ) {
                                ListItem(
                                    text = {
                                        Text(
                                            it.game!!,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    secondaryText = {
                                        Column {
                                            Text(
                                                it.info!!,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                it.runner!!,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    overlineText = {
                                        Text(
                                            it.time!!,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    icon = { it.startTimeReadable?.let { it1 -> Text(it1) } },
                                    trailing = {
                                        var toggle by remember { mutableStateOf(false) }
                                        IconToggleButton(
                                            checked = toggle,
                                            onCheckedChange = { toggle = it }
                                        ) {
                                            Icon(
                                                Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = animateColorAsState(
                                                    if (toggle && isCurrentGame) Color(0xFFe74c3c)
                                                    else LocalContentColor.current
                                                ).value
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@ExperimentalPagerApi
fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
    pageIndexMapping: (Int) -> Int = { it },
): Modifier = layout { measurable, constraints ->
    if (tabPositions.isEmpty()) {
        // If there are no pages, nothing to show
        layout(constraints.maxWidth, 0) {}
    } else {
        val currentPage = minOf(tabPositions.lastIndex, pageIndexMapping(pagerState.currentPage))
        val currentTab = tabPositions[currentPage]
        val previousTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffset
        val indicatorWidth = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.width, nextTab.width, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.width, previousTab.width, -fraction).roundToPx()
        } else {
            currentTab.width.roundToPx()
        }
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.left, nextTab.left, fraction).roundToPx()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.left, previousTab.left, -fraction).roundToPx()
        } else {
            currentTab.left.roundToPx()
        }
        val placeable = measurable.measure(
            Constraints(
                minWidth = indicatorWidth,
                maxWidth = indicatorWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )
        layout(constraints.maxWidth, maxOf(placeable.height, constraints.minHeight)) {
            placeable.place(
                indicatorOffset,
                maxOf(constraints.minHeight - placeable.height, 0)
            )
        }
    }
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiver(
    defaultValue: T,
    intentFilter: IntentFilter,
    tick: (context: Context, intent: Intent) -> T
): State<T> {
    val item: MutableState<T> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents.
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiverNullable(
    defaultValue: T?,
    intentFilter: IntentFilter,
    tick: (context: Context, intent: Intent) -> T?
): State<T?> {
    val item: MutableState<T?> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

/**
 * Creates a broadcast receiver that gets the time every tick that Android takes and
 * unregisters the receiver when the view is at the end of its lifecycle
 */
@Composable
fun currentTime(): State<Long> {
    return broadcastReceiver(
        defaultValue = System.currentTimeMillis(),
        intentFilter = IntentFilter(Intent.ACTION_TIME_TICK),
        tick = { _, _ -> System.currentTimeMillis() }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GDQScheduleTheme {
        GDQSchedule()
    }
}