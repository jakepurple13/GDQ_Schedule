package com.programmersbox.gdqschedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.gdqschedule.ui.theme.GDQScheduleTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationManager = getSystemService<NotificationManager>()
        val notificationName = "gdqschedule"
        val notificationChannel = NotificationChannel(
            "gdqschedule",
            notificationName,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationChannel.description = "gdq schedule"
        notificationManager?.createNotificationChannel(notificationChannel)

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
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val workManager = remember { WorkManager.getInstance(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val topBarState = rememberTopAppBarScrollState()
    val topBarBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SmallTopAppBar(title = { Text("Settings") })

            val w by workManager
                .getWorkInfosForUniqueWorkLiveData("gdqCurrentGame")
                .observeAsState(emptyList())

            var toggle by remember(w) { mutableStateOf(w.isNotEmpty()) }

            NavigationDrawerItem(
                label = { Text("Show Current Game Notification") },
                selected = false,
                onClick = {
                    toggle = !toggle
                    if (toggle) {
                        workManager.enqueueUniquePeriodicWork(
                            "gdqCurrentGame",
                            ExistingPeriodicWorkPolicy.REPLACE,
                            PeriodicWorkRequestBuilder<CurrentGameNotifier>(10, TimeUnit.MINUTES).build()
                        )
                    } else {
                        workManager.cancelUniqueWork("gdqCurrentGame")
                        context.getSystemService<NotificationManager>()?.cancel(13)
                    }
                },
                badge = { Switch(checked = toggle, onCheckedChange = null) }
            )

            NavigationDrawerItem(
                label = { Text("Clear all notifications") },
                selected = false,
                onClick = {
                    workManager.cancelAllWork()
                    workManager.pruneWork()
                },
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(topBarBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
                    title = { Text("GDQ Schedule") },
                    scrollBehavior = topBarBehavior,
                    actions = {
                        IconButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://gamesdonequick.com/donate"))
                                )
                            }
                        ) { Icon(Icons.Default.AttachMoney, null) }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    icons = { Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(currentTime)) },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.MenuOpen, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val pagerState = rememberPagerState()
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
                            contentPadding = PaddingValues(top = 2.dp, bottom = 2.dp)
                        ) {
                            val list = days.values.toList()[page]
                            itemsIndexed(list) { i, it ->
                                val d = Date(currentTime)
                                val isCurrentGame =
                                    d.after(it.startTimeAsDate) && d.before(list.getOrNull(i + 1)?.startTimeAsDate)
                                val cardColor =
                                    if (isCurrentGame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

                                ListItem(
                                    modifier = Modifier
                                        .border(
                                            4.dp,
                                            animateColorAsState(cardColor).value,
                                            MaterialTheme.shapes.small
                                        )
                                        .fillMaxWidth(),
                                    headlineText = { Text(it.game!!) },
                                    supportingText = {
                                        Column {
                                            Text(it.info!!)
                                            Text(it.runner!!)
                                        }
                                    },
                                    overlineText = { Text(it.time!!) },
                                    leadingContent = { it.startTimeReadable?.let { it1 -> Text(it1) } },
                                    trailingContent = if (d.before(it.startTimeAsDate)) {
                                        {
                                            val w by workManager
                                                .getWorkInfosForUniqueWorkLiveData("${it.game}${it.info}")
                                                .observeAsState(emptyList())

                                            var toggle by remember(w) { mutableStateOf(w.isNotEmpty()) }

                                            IconToggleButton(
                                                checked = toggle,
                                                onCheckedChange = { b ->
                                                    if (b) {
                                                        val delay =
                                                            it.startTimeAsDate?.time?.minus(currentTime) ?: 0L

                                                        workManager.enqueueUniqueWork(
                                                            "${it.game}${it.info}",
                                                            ExistingWorkPolicy.REPLACE,
                                                            OneTimeWorkRequestBuilder<GameNotifier>()
                                                                .setInputData(
                                                                    workDataOf(
                                                                        "gameInfo" to it.info,
                                                                        "gameTime" to it.startTimeReadable,
                                                                        "gameName" to it.game,
                                                                        "gameId" to "${it.game}${it.info}".hashCode()
                                                                    )
                                                                )
                                                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                                                .build()
                                                        )
                                                    } else {
                                                        workManager.cancelUniqueWork("${it.game}${it.info}")
                                                    }
                                                    toggle = b
                                                }
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
                                    } else null
                                )
                                Divider(modifier = Modifier.padding(top = 2.dp))
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

class GameNotifier(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("gameName")
        val time = inputData.getString("gameTime")
        val info = inputData.getString("gameInfo")
        val id = inputData.getInt("gameId", 0)

        val n = NotificationCompat.Builder(applicationContext, "gdqschedule")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setContentText(time)
            .setContentInfo(info)
            .build()

        applicationContext.getSystemService<NotificationManager>()?.notify(id, n)

        return Result.success()
    }
}

class CurrentGameNotifier(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val d = Date(currentTime)

        val data = Networking.getInfo().firstOrNull().orEmpty()

        var gameInfo: FullGameInfo? = null
        var index = 0

        for (i in data.withIndex()) {
            val it = i.value
            if (d.after(it.startTimeAsDate) && d.before(data.getOrNull(i.index + 1)?.startTimeAsDate)) {
                gameInfo = it
                index = i.index
                break
            }
        }

        val name = gameInfo?.game
        val time = gameInfo?.startTimeReadable
        val info = gameInfo?.info

        val next = data.getOrNull(index + 1)
        val second = data.getOrNull(index + 2)
        val third = data.getOrNull(index + 3)

        val n = NotificationCompat.Builder(applicationContext, "gdqschedule")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(name)
            .setContentText(time)
            .setContentInfo(info)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$time - $name")
                    .setSummaryText("$time - $name")
                    .bigText(
                        listOf(
                            "${next?.startTimeReadable} - ${next?.game}",
                            "${second?.startTimeReadable} - ${second?.game}",
                            "${third?.startTimeReadable} - ${third?.game}"
                        )
                            .joinToString("\n")
                    )
            )
            .setOngoing(true)
            .build()

        applicationContext.getSystemService<NotificationManager>()?.notify(13, n)

        return Result.success()
    }
}