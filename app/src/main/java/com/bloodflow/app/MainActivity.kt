package com.bloodflow.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val Ink = Color(0xFF322736)
private val Muted = Color(0xFF837889)
private val Rose = Color(0xFFE75B86)
private val RoseLight = Color(0xFFFFEAF0)
private val Lavender = Color(0xFF8E79D8)
private val Background = Color(0xFFFFF9FA)
private val Paper = Color.White
private val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")

data class Entry(val date: String, val mood: String, val symptoms: List<String>, val note: String)
data class Model(val periods: List<String> = emptyList(), val length: Int = 28, val days: Int = 5, val entries: List<Entry> = emptyList(), val name: String = "")

private fun readModel(ctx: Context): Model {
    return try {
    val raw = ctx.getSharedPreferences("bloodflow_local", Context.MODE_PRIVATE).getString("data", null) ?: return Model()
    val obj = JSONObject(raw)
    val ps = obj.optJSONArray("periods") ?: JSONArray()
    val es = obj.optJSONArray("entries") ?: JSONArray()
    Model(
        (0 until ps.length()).map { ps.getString(it) }.distinct().sortedDescending(),
        obj.optInt("length", 28).coerceIn(21, 45),
        obj.optInt("days", 5).coerceIn(1, 10),
        (0 until es.length()).map { i ->
            val e = es.getJSONObject(i)
            val sy = e.optJSONArray("symptoms") ?: JSONArray()
            Entry(e.getString("date"), e.optString("mood"), (0 until sy.length()).map { sy.getString(it) }, e.optString("note"))
        },
        obj.optString("name", "")
    )
} catch (_: Exception) { Model() }
}

private fun saveModel(ctx: Context, m: Model) {
    val o = JSONObject()
    o.put("periods", JSONArray(m.periods))
    o.put("length", m.length)
    o.put("days", m.days)
    o.put("name", m.name)
    o.put("entries", JSONArray().also { a -> m.entries.forEach { e ->
        a.put(JSONObject().put("date", e.date).put("mood", e.mood).put("symptoms", JSONArray(e.symptoms)).put("note", e.note))
    } })
    ctx.getSharedPreferences("bloodflow_local", Context.MODE_PRIVATE).edit().putString("data", o.toString()).commit()
}
private fun date(s: String) = LocalDate.parse(s)
private fun nice(s: String) = try { date(s).format(fmt) } catch (_: Exception) { s }
private fun cycleDay(last: LocalDate, now: LocalDate, length: Int): Int {
    val delta = ChronoUnit.DAYS.between(last, now)
    return if (delta < 0) 1 else (delta % length).toInt() + 1
}
private fun nextPeriod(last: LocalDate, today: LocalDate, length: Int): LocalDate {
    var result = last.plusDays(length.toLong())
    while (!result.isAfter(today)) result = result.plusDays(length.toLong())
    return result
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(255,249,250)
        window.navigationBarColor = android.graphics.Color.rgb(255,249,250)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            var state by remember { mutableStateOf(readModel(this)) }
            fun update(m: Model) { state = m; saveModel(this, m) }
            BloodflowApp(state, ::update)
        }
    }
}
@Composable
private fun BloodflowApp(model: Model, update: (Model) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("bloodflow_local", Context.MODE_PRIVATE) }
    var showIntro by remember { mutableStateOf(!prefs.getBoolean("intro_seen_v2", false)) }
    var tab by remember { mutableIntStateOf(0) }
    var onboarding by remember { mutableStateOf(model.periods.isEmpty()) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Rose, secondary = Lavender, background = Background, surface = Paper, onSurface = Ink)) {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            if (showIntro) {
                IntroCarousel(onComplete = { prefs.edit().putBoolean("intro_seen_v2", true).commit(); showIntro = false })
            } else if (onboarding) {
                Welcome(model) { name, start, length, duration ->
                    update(model.copy(name = name, periods = listOf(start.toString()), length = length, days = duration))
                    onboarding = false
                }
            } else {
                Scaffold(containerColor = Background, bottomBar = {
                    NavigationBar(containerColor = Paper) {
                        val tabs = listOf(Triple("Today", Icons.Default.FavoriteBorder, 0), Triple("Calendar", Icons.Default.DateRange, 1), Triple("Fertility", Icons.Default.Spa, 2), Triple("Journal", Icons.Default.EditNote, 3), Triple("You", Icons.Default.PersonOutline, 4))
                        tabs.forEach { (label, icon, id) ->
                            NavigationBarItem(selected = tab == id, onClick = { tab = id }, icon = { Icon(icon, label) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Rose, selectedTextColor = Rose, indicatorColor = RoseLight))
                        }
                    }
                }) { pad ->
                    Box(Modifier.padding(pad)) {
                        when (tab) {
                            0 -> TodayScreen(model, update) { tab = 3 }
                            1 -> CalendarScreen(model, update)
                            2 -> FertilityScreen(model)
                            3 -> JournalScreen(model, update)
                            else -> ProfileScreen(model, update) { onboarding = true }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Welcome(model: Model, onSave: (String, LocalDate, Int, Int) -> Unit) {
    var name by remember { mutableStateOf(model.name) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var length by remember { mutableFloatStateOf(28f) }
    var duration by remember { mutableFloatStateOf(5f) }
    var picker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = start.toEpochDay() * 86400000L)
    if (picker) DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
        TextButton(onClick = {
            dateState.selectedDateMillis?.let { val chosen = LocalDate.ofEpochDay(it / 86400000L); if (!chosen.isAfter(LocalDate.now())) start = chosen }
            picker = false
        }) { Text("Done") }
    }) { DatePicker(state = dateState) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE7EF),Background, Background))).padding(26.dp), verticalArrangement = Arrangement.Center) {
        Text("✦", fontSize = 46.sp, color = Rose)
        Spacer(Modifier.height(14.dp))
        Text("Your cycle,\nyour rhythm.", fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 43.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        Text("A gentle, private space to understand your cycle and how you feel.", color = Muted, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(32.dp))
        Panel {
            Text("What should we call you?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PremiumTextField(value = name, onValueChange = { name = it.take(40) }, label = "Your name or nickname", singleLine = true)
            Spacer(Modifier.height(18.dp))
            Text("When did your last period start?", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(10.dp)); Text(start.format(fmt)) }
            Spacer(Modifier.height(18.dp))
            Text("Usual cycle length  •  ${length.roundToInt()} days", fontWeight = FontWeight.SemiBold)
            Slider(value = length, onValueChange = { length = it }, valueRange = 21f..45f, steps = 23)
            Spacer(Modifier.height(10.dp))
            Text("Period length  •  ${duration.roundToInt()} days", fontWeight = FontWeight.SemiBold)
            Slider(value = duration, onValueChange = { duration = it }, valueRange = 1f..10f, steps = 8)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSave(name.trim(), start, length.roundToInt(), duration.roundToInt()) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Start my journey ✨", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(15.dp))
        Text("Your entries stay on this device. Predictions are estimates, not contraception or medical advice.", fontSize = 12.sp, color = Muted, lineHeight = 17.sp)
    }
}
@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Paper).padding(21.dp), content = content)
}
@Composable
private fun Header(kicker: String, headline: String) {
    Text(kicker.uppercase(), fontSize = 11.sp, letterSpacing = 1.7.sp, color = Rose, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
    Text(headline, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayScreen(m: Model, update: (Model) -> Unit, openJournal: () -> Unit) {
    val today = LocalDate.now()
    val last = date(m.periods.first())
    val day = cycleDay(last, today, m.length)
    val next = nextPeriod(last, today, m.length)
    val daysLeft = ChronoUnit.DAYS.between(today,next)
    var picker by remember { mutableStateOf(false) }
    val dp = rememberDatePickerState(initialSelectedDateMillis = today.toEpochDay() * 86400000L)
    if(picker) DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
        TextButton(onClick = {
            dp.selectedDateMillis?.let { millis ->
                val d = LocalDate.ofEpochDay(millis / 86400000L)
                if (!d.isAfter(today)) update(m.copy(periods = (m.periods + d.toString()).distinct().sortedDescending()))
            }
            picker = false
        }) { Text("Save") }
    }) { DatePicker(state = dp) }
    LazyColumn(contentPadding = PaddingValues(23.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) { Header("Bloodflow · daily companion", if (m.name.isBlank()) "Hello, beautiful ♡" else "Hello, ${m.name} ♡") }
                Box(Modifier.size(48.dp).clip(CircleShape).background(RoseLight), contentAlignment = Alignment.Center) { Text("✿",fontSize=26.sp,color=Rose) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Color(0xFFFBE2E9), Color(0xFFF2E7FD)))).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YOUR CYCLE AT A GLANCE", color = Ink, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(13.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(212.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(13.dp)) {
                        drawArc(Color.White.copy(alpha=.7f),0f,360f,false,style=Stroke(15.dp.toPx(),cap=StrokeCap.Round))
                        drawArc(brush=Brush.sweepGradient(listOf(Rose,Lavender,Rose)), startAngle=-90f,sweepAngle=360f*day/m.length, useCenter=false, style=Stroke(15.dp.toPx(),cap=StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DAY",fontSize=12.sp,color=Muted,letterSpacing=2.sp)
                        Text("$day",fontSize=61.sp,fontWeight=FontWeight.Bold,color=Ink,lineHeight=67.sp)
                        Text("of ${m.length}",fontSize=14.sp,color=Muted)
                    }
                }
                val phase = when {
                    day <= m.days -> "Menstrual phase"
                    day < (m.length - 14).coerceAtLeast(m.days + 1) -> "Follicular phase"
                    day <= (m.length - 13).coerceAtLeast(m.days + 2) -> "Around ovulation*"
                    else -> "Luteal phase"
                }
                Text(phase, fontSize=21.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Next period estimated in $daysLeft day${if(daysLeft==1L) "" else "s"}",fontSize=14.sp,color=Muted)
                Spacer(Modifier.height(8.dp))
                Text("*Cycle phases are approximations.",fontSize=11.sp,color=Muted)
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                StatCard("Next period",next.format(DateTimeFormatter.ofPattern("MMM d")),"Estimated",Modifier.weight(1f))
                StatCard("Cycle length","${m.length} days","Your setting",Modifier.weight(1f))
            }
        }
        item {
            Panel {
                Text("How are you feeling today?", fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("A little check-in can reveal your patterns over time.",fontSize=13.sp,color=Muted)
                Spacer(Modifier.height(14.dp))
                Button(onClick=openJournal,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)){ Icon(Icons.Default.EditNote,null);Spacer(Modifier.width(7.dp));Text("Log mood & symptoms") }
            }
        }
        item {
            Panel {
                Text("Period tracking",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Record the first day of each period to improve future estimates.",fontSize=13.sp,color=Muted)
                Spacer(Modifier.height(13.dp))
                OutlinedButton(onClick={picker=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp)); Text("Log period start") }
            }
        }
        item { Guidance() }
    }
}
@Composable
private fun StatCard(title: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Paper).padding(17.dp)) {
        Text(title,fontSize=12.sp,color=Muted);Spacer(Modifier.height(9.dp))
        Text(value,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
        Text(sub,fontSize=11.sp,color=Rose)
    }
}
@Composable
private fun Guidance() {
    Panel {
        Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome,null,tint=Lavender);Spacer(Modifier.width(9.dp));Text("A gentle reminder",fontSize=17.sp,fontWeight=FontWeight.Bold) }
        Spacer(Modifier.height(9.dp))
        Text("Cycle timing naturally varies. Predictions cannot confirm ovulation or identify safe days for avoiding pregnancy.",fontSize=13.sp,color=Muted,lineHeight=19.sp)
        Spacer(Modifier.height(8.dp))
        Text("Seek medical advice for very heavy bleeding, severe pain, bleeding between periods, or concerning changes.",fontSize=13.sp,color=Muted,lineHeight=19.sp)
    }
}
@Composable
private fun CalendarScreen(m: Model, update: (Model) -> Unit) {
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val today = LocalDate.now()
    val start = date(m.periods.first())
    val predicted = nextPeriod(start,today,m.length)
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    val periodDates = remember(m.periods,m.days) { m.periods.flatMap { s -> (0 until m.days).map { date(s).plusDays(it.toLong()) } }.toSet() }
    LazyColumn(contentPadding=PaddingValues(23.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
        item { Header("Your personal timeline","Cycle calendar") }
        item {
            Panel {
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    IconButton(onClick={month=month.minusMonths(1)}) { Icon(Icons.Default.ChevronLeft,null) }
                    Text(month.format(monthFmt),fontSize=18.sp,fontWeight=FontWeight.Bold)
                    IconButton(onClick={month=month.plusMonths(1)}) { Icon(Icons.Default.ChevronRight,null) }
                }
                Spacer(Modifier.height(15.dp))
                val weekdays = listOf("M","T","W","T","F","S","S")
                Row(Modifier.fillMaxWidth()) { weekdays.forEach { Text(it,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,color=Muted,fontSize=12.sp) } }
                Spacer(Modifier.height(13.dp))
                val offset = month.dayOfWeek.value - 1
                val count = month.lengthOfMonth()
                for(row in 0 until ((offset+count+6)/7)) {
                    Row(Modifier.fillMaxWidth()) {
                        for(col in 0..6) {
                            val n = row*7+col-offset+1
                            Box(Modifier.weight(1f).height(49.dp),contentAlignment=Alignment.Center) {
                                if(n in 1..count) {
                                    val d = month.withDayOfMonth(n)
                                    val observed = d in periodDates
                                    val forecast = !d.isBefore(predicted) && d.isBefore(predicted.plusDays(m.days.toLong()))
                                    val selected = d==today
                                    val fertility = assessFertility(m,d)
                                    val colored = if (observed) Rose else if (fertility.category == FertilityCategory.HIGHER) Color(0xFFFFE1B4) else if (forecast) RoseLight else if (selectedDay == d) Color(0xFFE4D8FC) else if(selected) Color(0xFFECE6F9) else Color.Transparent
                                    Box(Modifier.size(39.dp).clip(CircleShape).background(colored).clickable { selectedDay = d },contentAlignment=Alignment.Center) {
                                        Text("$n",color=if(observed) Color.White else Ink,fontWeight=if(selected||observed||selectedDay==d) FontWeight.Bold else FontWeight.Normal,fontSize=14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Legend(Rose,"Logged period");Legend(RoseLight,"Predicted")
                }
            }
        }
        item {
            Panel {
                val assessed = assessFertility(m,selectedDay)
                Text(selectedDay.format(fmt),fontWeight=FontWeight.Bold,fontSize=17.sp)
                Spacer(Modifier.height(6.dp))
                Text(assessed.title, color=if (assessed.category==FertilityCategory.HIGHER) Color(0xFF965522) else Muted, fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(assessed.explanation,fontSize=12.sp,color=Muted,lineHeight=18.sp)
                Spacer(Modifier.height(9.dp))
                Text("Amber days = estimated fertile window · NOT guaranteed safe on other dates",fontSize=11.sp,color=Muted)
            }
        }
        item {
            Panel {
                Text("Previous period starts",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                m.periods.take(10).forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("●",color=Rose);Spacer(Modifier.width(10.dp));Text(nice(p),modifier=Modifier.weight(1f),fontSize=14.sp)
                        if(m.periods.size>1) IconButton(onClick={update(m.copy(periods=m.periods.filterNot { it==p }))}) { Icon(Icons.Default.Close,"Delete period",tint=Muted) }
                    }
                }
            }
        }
        item { Guidance() }
    }
}
@Composable private fun Legend(c:Color,label:String) { Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(c));Spacer(Modifier.width(5.dp));Text(label,fontSize=11.sp,color=Muted) } }

private val moods = listOf("😊 Good","😌 Calm","😐 Okay","😔 Low","😣 Tough")
private val symptoms = listOf("Cramps","Bloating","Headache","Fatigue","Tender breasts","Acne","Back pain","Cravings","Spotting","Nausea")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalScreen(m: Model, update: (Model) -> Unit) {
    val today = LocalDate.now().toString()
    val existing = m.entries.firstOrNull { it.date == today }
    var mood by remember(existing) { mutableStateOf(existing?.mood ?: "") }
    var selected by remember(existing) { mutableStateOf(existing?.symptoms?.toSet() ?: emptySet()) }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var saved by remember { mutableStateOf(false) }
    LazyColumn(contentPadding=PaddingValues(23.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("Listen to your body","Daily journal") }
        item {
            Panel {
                Text("How's your mood?",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(11.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) { moods.forEach { v ->
                    FilterChip(selected=mood==v,onClick={mood=v;saved=false},label={Text(v)})
                } }
            }
        }
        item {
            Panel {
                Text("What are you noticing?",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Select any symptoms.",fontSize=12.sp,color=Muted)
                Spacer(Modifier.height(9.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) { symptoms.forEach { v ->
                    FilterChip(selected=v in selected,onClick={ selected=if(v in selected)selected-v else selected+v;saved=false },label={Text(v)})
                } }
            }
        }
        item {
            Panel {
                Text("A note to yourself",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value=note,onValueChange={note=it.take(2000);saved=false},label="Your private thoughts",singleLine=false,minLines=3,maxLines=6)
                Spacer(Modifier.height(16.dp))
                Button(onClick={
                    val e = Entry(today,mood,selected.toList().sorted(),note)
                    update(m.copy(entries=(m.entries.filterNot{it.date==today}+e).sortedByDescending{it.date}))
                    saved=true
                },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Text(if(saved) "Saved ✓" else "Save today's check-in") }
            }
        }
        item {
            Panel {
                Text("Your previous entries",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(11.dp))
                if(m.entries.isEmpty()) Text("Your check-ins will appear here.",color=Muted,fontSize=13.sp)
                m.entries.take(30).forEach { e ->
                    HorizontalDivider(color=Background)
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nice(e.date),fontWeight=FontWeight.SemiBold)
                            Text(listOf(e.mood,e.symptoms.joinToString(", "),e.note).filter{it.isNotBlank()}.joinToString(" · ").take(160),fontSize=12.sp,color=Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
                        }
                        IconButton(onClick={update(m.copy(entries=m.entries.filterNot{it.date==e.date}))}) { Icon(Icons.Default.DeleteOutline,"Delete check-in",tint=Muted) }
                    }
                    Spacer(Modifier.height(11.dp))
                }
            }
        }
    }
}
@Composable
private fun ProfileScreen(m: Model, update: (Model) -> Unit, reset: () -> Unit) {
    var length by remember(m.length) { mutableFloatStateOf(m.length.toFloat()) }
    var duration by remember(m.days) { mutableFloatStateOf(m.days.toFloat()) }
    var profileName by remember(m.name) { mutableStateOf(m.name) }
    var confirm by remember { mutableStateOf(false) }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Erase all your data?")},text={Text("This permanently removes all period history, symptoms, notes, and settings saved by Bloodflow on this device.")},confirmButton={
        TextButton(onClick={update(Model());confirm=false;reset()}) { Text("Erase everything",color=Rose) }
    },dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
    LazyColumn(contentPadding=PaddingValues(23.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Header("MADE FOR YOU · v1.2", if (m.name.isBlank()) "Your profile" else "${m.name}’s space") }
        item {
            Panel {
                Text("Make Bloodflow yours", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                PremiumTextField(value = profileName, onValueChange = { profileName = it.take(40) }, label = "Your name or nickname", singleLine = true)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { update(m.copy(name = profileName.trim())) }, modifier = Modifier.fillMaxWidth()) { Text("Save my name") }
            }
        }
        item {
            Panel {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("🪷",fontSize=33.sp);Spacer(Modifier.width(12.dp));Column { Text("Bloodflow",fontSize=22.sp,fontWeight=FontWeight.Bold);Text(if (m.name.isBlank()) "A more mindful monthly rhythm" else "Made for ${m.name} ♡",fontSize=12.sp,color=Muted) } }
            }
        }
        item {
            Panel {
                Text("Cycle preferences",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Typical cycle length: ${length.roundToInt()} days")
                Slider(value=length,onValueChange={length=it},valueRange=21f..45f,steps=23)
                Text("Typical period length: ${duration.roundToInt()} days")
                Slider(value=duration,onValueChange={duration=it},valueRange=1f..10f,steps=8)
                Button(onClick={update(m.copy(length=length.roundToInt(),days=duration.roundToInt()))},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Text("Save preferences") }
            }
        }
        item {
            Panel {
                Text("Your privacy matters",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text("Your information is stored in private app storage on this device. Bloodflow uses no account, advertising SDK, analytics, or internet permission. Uninstalling the app or clearing app data removes your history. This version does not encrypt its local data.",fontSize=13.sp,color=Muted,lineHeight=19.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteForever,null);Spacer(Modifier.width(8.dp));Text("Delete all my data") }
            }
        }
        item {
            Panel {
                Text("About predictions",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Predictions are calendar-based estimates using your selected cycle length. They are not diagnoses, ovulation tests, fertility measurements, pregnancy tests, or a reliable method of birth control. Different cycles and ages can have different normal ranges.",fontSize=13.sp,color=Muted,lineHeight=19.sp)
                Spacer(Modifier.height(9.dp))
                Text("Version 1.2.0 · Offline-first",fontSize=12.sp,color=Rose)
            }
        }
    }
}
