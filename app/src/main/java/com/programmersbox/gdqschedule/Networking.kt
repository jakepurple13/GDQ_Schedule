package com.programmersbox.gdqschedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

object Networking {
    val url = "https://gamesdonequick.com/schedule"

    suspend fun getInfo() = flow {
        Jsoup.connect(url).get()
            .select("table#runTable")
            .select("tr, tr.second-row")
            .drop(1)
            .mapIndexedNotNull { index, g ->
                try {
                    if (index % 2 == 0) {
                        GameInfo.Game(
                            game = g.select("tr > td")[1].text(),
                            runner = g.select("tr > td")[2].text(),
                            startTime = g.select("tr > td.start-time").text()
                        )
                    } else {
                        GameInfo.Info(
                            time = g.select("tr.second-row > td.text-right").text(),
                            info = g.select("tr.second-row > td")[1].text()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            .let {
                it.fold(mutableListOf<FullGameInfo>()) { m, i ->
                    val f = when (i) {
                        is GameInfo.Game -> FullGameInfo(i.game, i.runner, i.startTime)
                        is GameInfo.Info -> {
                            val g = m.removeLastOrNull()
                            g?.copy(time = i.time, info = i.info)
                        }
                    }
                    f?.let { it1 -> m.add(it1) }
                    m
                }
            }
            .let { emit(it) }
    }
}

sealed class GameInfo {
    data class Game(
        val game: String,
        val runner: String,
        val startTime: String
    ) : GameInfo()

    data class Info(
        val time: String,
        val info: String
    ) : GameInfo()
}

data class FullGameInfo(
    val game: String?,
    val runner: String?,
    val startTime: String?,
    val time: String? = null,
    val info: String? = null
) {
    val startTimeReadable: String?
        get() {
            val s = startTime?.let {
                val ta = DateTimeFormatter.ISO_INSTANT.parse(it)
                val i = Instant.from(ta)
                Date.from(i)
            }
                ?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) }

            return s
        }

}

class GameViewModel : ViewModel() {

    var gameInfo by mutableStateOf(emptyList<FullGameInfo>())

    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    val gameInfoGrouped
        get() = gameInfo.groupBy {
            val ta = DateTimeFormatter.ISO_INSTANT.parse(it.startTime!!)
            val i = Instant.from(ta)
            val d = Date.from(i)
            dateFormat.format(d)
        }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Networking.getInfo()
                .onEach { gameInfo = it }
                .collect()
        }
    }

}