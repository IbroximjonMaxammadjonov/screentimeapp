package com.example.screentimeapp
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var screenTimeReceiver: ScreenTimeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("ScreenTimePrefs", Context.MODE_PRIVATE)

        // BroadcastReceiver ni faqat ilova ishga tushganda ro‘yxatdan o‘tkazamiz
        screenTimeReceiver = ScreenTimeReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenTimeReceiver, filter)

        setContent {
            ScreenTimeApp(sharedPreferences)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenTimeReceiver)
    }
}


@Composable
fun ScreenTimeApp(sharedPreferences: SharedPreferences) {
    var totalScreenTime by remember { mutableStateOf(sharedPreferences.getLong("totalScreenTime", 0)) }
    var isScreenOn by remember { mutableStateOf(sharedPreferences.getBoolean("isScreenOn", false)) }
    var lastScreenOnTime by remember { mutableStateOf(sharedPreferences.getLong("lastScreenOnTime", 0)) }

    // 📌 UI har soniyada avtomatik yangilanadi
    LaunchedEffect(isScreenOn) {
        while (isScreenOn) {
            val currentTime = System.currentTimeMillis()
            totalScreenTime = sharedPreferences.getLong("totalScreenTime", 0) + (currentTime - lastScreenOnTime)
            delay(1000) // ✅ delay ishlaydi, chunki LaunchedEffect suspend funksiya ichida
        }
    }

    val hours = (totalScreenTime / (1000 * 60 * 60))
    val minutes = (totalScreenTime / (1000 * 60)) % 60
    val seconds = (totalScreenTime / 1000) % 60

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isScreenOn) "Ekran hozir yoqilgan 🔵" else "Ekran o‘chirilgan ⚫",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$hours soat $minutes daqiqa $seconds soniya",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            sharedPreferences.edit().putLong("totalScreenTime", 0).commit()
            totalScreenTime = 0
            Log.d("ScreenTimeApp", "⏳ Reset tugmasi bosildi, vaqt nollandi")
        }) {
            Text("Reset qilish")
        }
    }
}





// 📌 Bugungi sanani olish funksiyasi
fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(Date())
}

class ScreenTimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenTimeReceiver", "🟢 Received intent: ${intent.action}")

        val sharedPreferences = context.getSharedPreferences("ScreenTimePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentTime = System.currentTimeMillis()

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                editor.putLong("lastScreenOnTime", currentTime)
                editor.putBoolean("isScreenOn", true) // 🔄 UIga signal berish uchun
                editor.commit()
                Log.d("ScreenTimeReceiver", "✅ SCREEN ON - Time saved: $currentTime")
            }
            Intent.ACTION_SCREEN_OFF -> {
                val lastScreenOnTime = sharedPreferences.getLong("lastScreenOnTime", -1)
                val totalScreenTime = sharedPreferences.getLong("totalScreenTime", 0)

                if (lastScreenOnTime > 0 && lastScreenOnTime < currentTime) {
                    val screenOnDuration = currentTime - lastScreenOnTime
                    val newTotalScreenTime = totalScreenTime + screenOnDuration

                    editor.putLong("totalScreenTime", newTotalScreenTime)
                    editor.putBoolean("isScreenOn", false) // 🔄 UIga signal berish uchun
                    editor.commit()

                    Log.d("ScreenTimeReceiver", "✅ SCREEN OFF - Added time: $screenOnDuration, Total time: $newTotalScreenTime")
                } else {
                    Log.e("ScreenTimeReceiver", "❌ ERROR: lastScreenOnTime noto‘g‘ri (${lastScreenOnTime}), vaqt saqlanmaydi.")
                }
            }
        }
    }
}






