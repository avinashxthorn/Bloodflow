package com.bloodflow.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Plum = Color(0xFF43243E)
private val Pink = Color(0xFFE95691)
private val Violet = Color(0xFF9670DB)
private val Dust = Color(0xFF7D6A7B)

enum class FertilityCategory { HIGHER, OUTSIDE_ESTIMATE, UNCERTAIN }
data class FertilityEstimate(val category: FertilityCategory, val title: String, val explanation: String, val cycleDay: Int?)

fun assessFertility(model: Model, selected: LocalDate): FertilityEstimate {
    if (model.periods.isEmpty()) return FertilityEstimate(FertilityCategory.UNCERTAIN, "Not enough data", "Log a period to see calendar estimates.", null)
    val starts = model.periods.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.filter { !it.isAfter(selected) }.sorted()
    if (starts.isEmpty()) return FertilityEstimate(FertilityCategory.UNCERTAIN, "Not enough data", "There is no period start before this day.", null)
    val latest = starts.last()
    val daysSince = ChronoUnit.DAYS.between(latest, selected)
    val day = (daysSince % model.length).toInt() + 1
    val recentIntervals = model.periods.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .sorted().zipWithNext { a,b -> ChronoUnit.DAYS.between(a,b) }
    val regular = model.length in 26..32 && recentIntervals.all { it in 26L..32L }
    if (!regular) return FertilityEstimate(FertilityCategory.UNCERTAIN, "Timing is uncertain",
        "The Standard Days Method applies only to regular cycles of 26–32 days. Calendar dates cannot identify safe sex days.", day)
    if (daysSince >= model.length * 2L && model.periods.size < 2) return FertilityEstimate(FertilityCategory.UNCERTAIN,
        "Older cycle information", "Log a recent period to refresh estimates. No date is guaranteed safe for unprotected sex.", day)
    return if (day in 8..19) FertilityEstimate(FertilityCategory.HIGHER,
        "Potentially fertile — days 8–19",
        "In regular 26–32-day cycles, this is the Standard Days Method's potentially fertile window. Avoid unprotected sex if pregnancy is not desired.", day)
    else FertilityEstimate(FertilityCategory.OUTSIDE_ESTIMATE,
        "Outside estimated fertile window",
        "Pregnancy is still possible. This is not a safe day for unprotected sex: ovulation can shift, and this estimate is not contraception.", day)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    minLines: Int = 1,
    maxLines: Int = 6
) {
    OutlinedTextField(
        value=value, onValueChange=onValueChange,
        label={ Text(label) }, placeholder={ Text(if(singleLine) "Type here…" else "Write how you feel…") },
        singleLine=singleLine, minLines=minLines, maxLines=maxLines,
        shape=RoundedCornerShape(20.dp),
        modifier=Modifier.fillMaxWidth(),
        colors=OutlinedTextFieldDefaults.colors(
            focusedBorderColor=Pink, unfocusedBorderColor=Color(0xFFF3DDE7),
            focusedContainerColor=Color(0xFFFFF6FB), unfocusedContainerColor=Color(0xFFFFF9FC),
            cursorColor=Pink, focusedLabelColor=Pink
        )
    )
}

@Composable
fun IntroCarousel(onComplete: () -> Unit) {
    val pager = rememberPagerState(pageCount={3})
    val scope = rememberCoroutineScope()
    val names=listOf("Your cycle, your rhythm","Notice your patterns","Knowledge, not guesswork")
    val details=listOf(
        "Record periods, see your calendar and learn when the next period may begin.",
        "Keep a private mood, symptom and notes journal to spot changes month to month.",
        "View estimated fertile days with clear warnings: there are no guaranteed safe days for unprotected sex."
    )
    val icons=listOf(Icons.Default.Favorite, Icons.Default.AutoAwesome, Icons.Default.Shield)
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE4F0),Color(0xFFFFFBFE),Color(0xFFF2E8FF)))).padding(horizontal=24.dp,vertical=26.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Text("✿  Bloodflow",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Plum,modifier=Modifier.weight(1f))
            TextButton(onClick=onComplete) { Text("Skip",color=Dust) }
        }
        Spacer(Modifier.height(28.dp))
        HorizontalPager(state=pager,modifier=Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Box(
                    Modifier.size(220.dp).clip(RoundedCornerShape(64.dp)).background(
                        Brush.linearGradient(listOf(Color(0xFFFF8EAE),Color(0xFFBF8FE9)))),
                    contentAlignment=Alignment.Center
                ) {
                    Icon(icons[page],contentDescription=null,modifier=Modifier.size(125.dp),tint=Color.White)
                }
                Spacer(Modifier.height(44.dp))
                Text(names[page],fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,color=Plum)
                Spacer(Modifier.height(17.dp))
                Text(details[page],fontSize=17.sp,lineHeight=25.sp,textAlign=TextAlign.Center,color=Dust)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically) {
            repeat(3) { i ->
                Box(Modifier.size(if(i==pager.currentPage) 25.dp else 9.dp,9.dp).clip(CircleShape)
                    .background(if(i==pager.currentPage) Pink else Color(0xFFEACDDE)))
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick={ if (pager.currentPage == 2) onComplete() else scope.launch { pager.animateScrollToPage(pager.currentPage+1) }; Unit },
            modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(22.dp),
            colors=ButtonDefaults.buttonColors(containerColor=Pink)
        ) { Text(if(pager.currentPage==2) "Get started" else "Next  →",fontSize=17.sp,fontWeight=FontWeight.Bold) }
        Spacer(Modifier.height(20.dp))
        Text("Private by design · Your data stays on this device",fontSize=12.sp,color=Dust,textAlign=TextAlign.Center)
    }
}

@Composable
fun FertilityScreen(model: Model) {
    val today=LocalDate.now()
    val current=assessFertility(model,today)
    var offset by remember { mutableIntStateOf(0) }
    val selected=today.plusDays(offset.toLong())
    val estimate=assessFertility(model,selected)
    val amber=Color(0xFFFFEDCC)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("BODY LITERACY",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Pink,letterSpacing=1.5.sp)
        Text("Fertility awareness",fontSize=29.sp,fontWeight=FontWeight.Bold,color=Plum)
        Text("Understand estimated timing without false reassurance.",fontSize=14.sp,color=Dust)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
            Brush.linearGradient(listOf(Color(0xFFFFE6ED),Color(0xFFF1E4FF)))).padding(23.dp)) {
            Icon(Icons.Default.CalendarMonth,null,tint=Pink,modifier=Modifier.size(32.dp))
            Spacer(Modifier.height(14.dp))
            Text("TODAY · CYCLE DAY ${current.cycleDay ?: "—"}",color=Dust,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(current.title,color=Plum,fontSize=21.sp,fontWeight=FontWeight.Bold,lineHeight=27.sp)
            Spacer(Modifier.height(10.dp))
            Text(current.explanation,fontSize=14.sp,lineHeight=21.sp,color=Plum)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
            Text("Explore dates",fontWeight=FontWeight.Bold,fontSize=20.sp,color=Plum)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
                IconButton(onClick={offset=(offset-1).coerceAtLeast(-60)}) { Icon(Icons.Default.ChevronLeft,"Previous") }
                Text(selected.toString(),modifier=Modifier.weight(1f),textAlign=TextAlign.Center,fontWeight=FontWeight.SemiBold)
                IconButton(onClick={offset=(offset+1).coerceAtMost(60)}) { Icon(Icons.Default.ChevronRight,"Next") }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(
                if(estimate.category==FertilityCategory.HIGHER) amber else Color(0xFFF7EEF9)).padding(14.dp)) {
                Column {
                    Text(estimate.title,color=Plum,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(estimate.explanation,color=Dust,fontSize=13.sp,lineHeight=19.sp)
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
            Text("How the estimate works",fontWeight=FontWeight.Bold,fontSize=19.sp,color=Plum)
            Spacer(Modifier.height(12.dp))
            Text("• For consistently regular cycles of 26–32 days, cycle days 8–19 are treated as potentially fertile by the Standard Days Method.\n\n• At other cycle lengths, or when timing is uncertain, Bloodflow does not show a lower-likelihood classification.\n\n• A changing ovulation date can lead to pregnancy on unexpectedly fertile days.",fontSize=14.sp,lineHeight=21.sp,color=Dust)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(amber).padding(20.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber,null,tint=Color(0xFF97591E)); Spacer(Modifier.width(9.dp))
                Text("Important pregnancy prevention note",fontWeight=FontWeight.Bold,color=Plum)
            }
            Spacer(Modifier.height(10.dp))
            Text("No date is guaranteed safe for unprotected sex. If avoiding pregnancy, use a reliable contraceptive method. Condoms also reduce the risk of many sexually transmitted infections. This is not an ovulation test or medical diagnosis.",
                fontSize=14.sp,lineHeight=21.sp,color=Plum)
        }
        Text("Sources: CDC Fertility Awareness-Based Methods; ACOG fertility awareness guidance.",fontSize=12.sp,color=Dust)
    }
}
