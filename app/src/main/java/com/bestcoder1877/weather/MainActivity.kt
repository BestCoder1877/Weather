package com.bestcoder1877.weather

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestcoder1877.weather.ui.theme.WeatherTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@Composable
fun App(modifier: Modifier = Modifier) {
    var swiped by remember { mutableStateOf(false) }
    var lat by remember { mutableDoubleStateOf(0.0) }
    var lon by remember { mutableDoubleStateOf(0.0) }
    val context = LocalContext.current

    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var forecastTimes by remember { mutableStateOf<List<LocalDateTime>>(emptyList()) }
    var selectedHour by remember { mutableIntStateOf(0) }
    var lastTriggeredHour by remember { mutableIntStateOf(Int.MIN_VALUE) }
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    val locationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                }
            }
        }

    val hours = remember(forecastTimes, currentDateTime) {
        forecastTimes.map { dateTime ->
            if (dateTime.toLocalDate() == currentDateTime.toLocalDate() && dateTime.hour == currentDateTime.hour) {
                "NOW"
            } else {
                val displayHour = when {
                    dateTime.hour == 0 -> 12
                    dateTime.hour > 12 -> dateTime.hour - 12
                    else -> dateTime.hour
                }
                val day = when {
                    dateTime.toLocalDate() == currentDateTime.toLocalDate() -> ""
                    dateTime.toLocalDate() == currentDateTime.toLocalDate().plusDays(1) -> "TOM "
                    else -> "${dateTime.dayOfWeek.name.take(3)} "
                }
                "$day$displayHour ${if (dateTime.hour < 12) "AM" else "PM"}"
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            val now = currentDateTime
            val secondsUntilNextHour = (60 - now.minute) * 60 - now.second
            delay((secondsUntilNextHour * 1000L).milliseconds)
        }
    }

    LaunchedEffect(Unit) {
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var temperature by remember { mutableStateOf("__") }
    var feelsLike by remember { mutableStateOf("__") }
    var weatherCode by remember { mutableIntStateOf(0) }
    var temperatures by remember { mutableStateOf<List<String>>(emptyList()) }
    var feels by remember { mutableStateOf<List<String>>(emptyList()) }
    var codes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTemperature by remember { mutableStateOf("__") }
    var currentFeelsLike by remember { mutableStateOf("__") }
    var currentWeatherCode by remember { mutableIntStateOf(0) }

    suspend fun refresh() {
        if (lat == 0.0 && lon == 0.0) return
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,apparent_temperature,weather_code&hourly=temperature_2m,apparent_temperature,weather_code&forecast_days=7&timezone=auto"
        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)
        val current = json.getJSONObject("current")
        currentTemperature = current.getDouble("temperature_2m").toString()
        currentFeelsLike = current.getDouble("apparent_temperature").toString()
        currentWeatherCode = current.getInt("weather_code")
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val feelsArray = hourly.getJSONArray("apparent_temperature")
        val codesArray = hourly.getJSONArray("weather_code")
        forecastTimes = (0 until times.length()).map { LocalDateTime.parse(times.getString(it)) }
        temperatures = (0 until temps.length()).map { temps.getString(it) }
        feels = (0 until feelsArray.length()).map { feelsArray.getString(it) }
        codes = (0 until codesArray.length()).map { codesArray.getInt(it) }
        val now = LocalDateTime.now()
        val currentIndex = forecastTimes.indexOfFirst { time ->
            time.year == now.year && time.monthValue == now.monthValue && time.dayOfMonth == now.dayOfMonth && time.hour == now.hour
        }
        if (currentIndex >= 0) selectedHour = currentIndex
    }

    LaunchedEffect(lat, lon) { refresh() }

    LaunchedEffect(
        selectedHour,
        forecastTimes,
        currentTemperature,
        currentFeelsLike,
        currentWeatherCode
    ) {
        if (selectedHour in forecastTimes.indices) {
            val selectedTime = forecastTimes[selectedHour]
            val now = LocalDateTime.now()
            val isNow =
                selectedTime.year == now.year && selectedTime.monthValue == now.monthValue && selectedTime.dayOfMonth == now.dayOfMonth && selectedTime.hour == now.hour
            if (isNow) {
                temperature = currentTemperature
                feelsLike = currentFeelsLike
                weatherCode = currentWeatherCode
            } else {
                temperature = temperatures[selectedHour]
                feelsLike = feels[selectedHour]
                weatherCode = codes[selectedHour]
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5.minutes)
            refresh()
        }
    }

    val background = R.drawable.clear
    val jetbrainsmono = FontFamily(Font(R.font.jetbrains_mono_regular))
    val textOffset by animateDpAsState(
        targetValue = if (swiped) 300.dp else 0.dp,
        animationSpec = tween(800),
        label = ""
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragEnd = { swiped = !swiped }) { _, _ -> }
            }) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )
        WeatherEffects(weatherCode, Modifier.fillMaxSize())
        val scale by animateFloatAsState(
            targetValue = if (swiped) 10f else 1f,
            animationSpec = tween(800),
            label = ""
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(this@BoxWithConstraints.maxWidth * 10)
                    .offset(y = -this@BoxWithConstraints.maxWidth / 2)
                    .scale(scale)
                    .background(color = Color(0xFFC2D52D), shape = CircleShape)
            )
            val lazyListState = rememberLazyListState()
            LaunchedEffect(hours) {
                if (hours.isEmpty()) return@LaunchedEffect
                val targetIndex = 1200 + selectedHour.coerceIn(0, hours.lastIndex)
                lazyListState.scrollToItem(targetIndex)
                withFrameNanos { }
                val viewportWidth = lazyListState.layoutInfo.viewportSize.width
                val itemWidth =
                    lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }?.size
                        ?: 0
                if (viewportWidth > 0 && itemWidth > 0) lazyListState.scrollBy(-((viewportWidth - itemWidth) / 2f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-80).dp)
                    .alpha(if (swiped) 0f else 1f)
            ) {
                CircularCarouselList(
                    lazyListState = lazyListState,
                    items = hours.ifEmpty { listOf("") },
                    totalItems = 2400,
                    startIndex = 1200,
                    itemSize = 84.dp,
                    visualItemSize = 84.dp,
                    radiusPx = 800f,
                    density = LocalDensity.current,
                    fontFamily = jetbrainsmono,
                    onSelectedHour = { index ->
                        if (index in temperatures.indices && index != selectedHour) {
                            selectedHour = index
                            if (index != lastTriggeredHour) {
                                lastTriggeredHour = index
                                vibrator?.vibrate(VibrationEffect.createOneShot(40, 255))
                            }
                        }
                    })
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = textOffset)
            ) {
                Text(
                    text = "${temperature}°C",
                    modifier = Modifier
                        .padding(top = 200.dp)
                        .fillMaxWidth(),
                    style = TextStyle(
                        fontSize = 96.sp,
                        fontFamily = jetbrainsmono,
                        color = Color.Black,
                        fontWeight = FontWeight(400),
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = "Feels Like ${feelsLike}°C",
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = jetbrainsmono,
                        fontWeight = FontWeight(400),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                )
                val selectedTime = forecastTimes.getOrNull(selectedHour) ?: currentDateTime
                val now = LocalDateTime.now()
                val dayLabel = when {
                    selectedTime.toLocalDate() == now.toLocalDate() -> "Today"
                    selectedTime.toLocalDate() == now.toLocalDate().plusDays(1) -> "Tomorrow"
                    else -> selectedTime.dayOfWeek.name.take(3)
                }
                val hour12 = if (selectedTime.hour % 12 == 0) 12 else selectedTime.hour % 12
                val amPm = if (selectedTime.hour < 12) "AM" else "PM"
                Text(
                    text = "$dayLabel at $hour12 $amPm",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp, bottom = 8.dp)
                        .alpha(if (swiped) 1f else 0f),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontFamily = jetbrainsmono,
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight(500),
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    WeatherTheme { App() }
}
