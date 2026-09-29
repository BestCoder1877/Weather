package com.bestcoder1877.weather

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.faskn.composeplayground.carousel.CircularCarouselList
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(
                        modifier = Modifier.padding(innerPadding)
                    )
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

    var currentHour = LocalDateTime.now().hour

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationClient =
            LocationServices.getFusedLocationProviderClient(context)

        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lat = location.latitude
                lon = location.longitude
            }
        }
    }

    val hours = remember {
        (0..23).map { hour ->
            if (hour == currentHour) {
                "NOW"
            } else {
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$displayHour ${if (hour < 12) "AM" else "PM"}"
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentHour = LocalDateTime.now().hour
            val now = LocalDateTime.now()
            val secondsUntilNextHour =
                (60 - now.minute) * 60 - now.second
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

    LaunchedEffect(lat, lon) {
        if (lat != 0.0 && lon != 0.0) {
            val url =
                "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat" +
                        "&longitude=$lon" +
                        "&current=temperature_2m,apparent_temperature,weather_code"
            val response = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            val current = JSONObject(response).getJSONObject("current")
            weatherCode = current.getInt("weather_code")
            temperature = current.getDouble("temperature_2m").toString()
            feelsLike = current.getDouble("apparent_temperature").toString()
        }
    }

    val background = when (weatherCode) {
        0 -> R.drawable.clear
        1 -> R.drawable.partly_cloudy
        2 -> R.drawable.partly_cloudy
        3 -> R.drawable.overcast
        45, 48 -> R.drawable.fog
        51, 53, 55,
        56, 57,
        61, 63, 65,
        66, 67,
        80, 81, 82 -> R.drawable.rain

        71, 73, 75,
        77, 85, 86 -> R.drawable.snow

        95, 96, 99 -> R.drawable.storm
        else -> R.drawable.clear
    }

    val jetbrainsmono = FontFamily(
        Font(R.font.jetbrains_mono_regular)
    )
    val textOffset by animateDpAsState(
        targetValue = if (swiped) 300.dp else 0.dp,
        animationSpec = tween(800),
        label = ""
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swiped = !swiped }
                ) { _, _ -> }
            }
    ) {
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

        val scale by animateFloatAsState(
            targetValue = if (swiped) 10f else 1f,
            animationSpec = tween(800),
            label = ""
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(this@BoxWithConstraints.maxWidth * 10)
                    .offset(y = -this@BoxWithConstraints.maxWidth / 2)
                    .scale(scale)
                    .background(
                        color = Color(0xFFC2D52D),
                        shape = CircleShape
                    )
            )
            if (!swiped) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = (-80).dp)
                ) {


                    val currentHour = LocalDateTime.now().hour
                    val lazyListState = rememberLazyListState()
                    LaunchedEffect(currentHour) {
                        val targetIndex = 1200 + currentHour
                        lazyListState.scrollToItem(
                            index = targetIndex
                        )
                        val viewportWidth =
                            lazyListState.layoutInfo.viewportSize.width
                        val itemWidth =
                            lazyListState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == targetIndex }
                                ?.size
                                ?: 0
                        if (viewportWidth > 0 && itemWidth > 0) {
                            lazyListState.scrollBy(
                                -((viewportWidth - itemWidth) / 2f)
                            )
                        }
                    }

                    CircularCarouselList(
                        lazyListState = lazyListState,
                        items = hours,
                        itemSize = 84.dp,
                        visualItemSize = 84.dp,
                        radiusPx = 800f,
                        density = LocalDensity.current,
                        fontFamily = jetbrainsmono
                    )


                }
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
                        textAlign = TextAlign.Center,
                    )
                )
            }


        }
    }
}


@Preview(showBackground = true)
@Composable
fun AppPreview() {
    WeatherTheme {
        App()
    }
}