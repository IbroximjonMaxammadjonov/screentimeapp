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

        sharedPreferences = getSharedPreferences("ScreenTimePrefs", MODE_PRIVATE)

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
    var totalScreenTime by remember { mutableLongStateOf(sharedPreferences.getLong("totalScreenTime", 0)) }
    var isScreenOn by remember { mutableStateOf(sharedPreferences.getBoolean("isScreenOn", false)) }

    // 📌 UI har soniyada avtomatik yangilanadi
    LaunchedEffect(isScreenOn) {
        while (isScreenOn) {
            val currentTime = System.currentTimeMillis()
            val lastSavedTime = sharedPreferences.getLong("totalScreenTime", 0)
            val lastScreenOn = sharedPreferences.getLong("lastScreenOnTime", 0)

            totalScreenTime = lastSavedTime + (currentTime - lastScreenOn)

            Log.d("ScreenTimeApp", "📊 UI yangilandi: $totalScreenTime ms")
            delay(1000) // ✅ Har 1 soniyada UI yangilanadi
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
            text = "Bugungi ekran yoniq vaqti:",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$hours soat $minutes daqiqa $seconds soniya",
            fontSize = 24.sp
        )
    }
}


class ScreenTimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenTimeReceiver", "🟢 Received intent: ${intent.action}")

        val sharedPreferences = context.getSharedPreferences("ScreenTimePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentTime = System.currentTimeMillis()
        val currentDate = getCurrentDate() // 📌 Hozirgi kunni olish

        val savedDate = sharedPreferences.getString("lastSavedDate", "") // 📌 Saqlangan sana
        if (savedDate != currentDate) {
            Log.d("ScreenTimeReceiver", "🔄 Yangi kun boshlandi, vaqtni reset qilamiz.")
            editor.putLong("totalScreenTime", 0) // 🔄 Kun boshida reset
            editor.putLong("lastScreenOnTime", 0) // 🔄 Eski vaqtni tozalash
            editor.putString("lastSavedDate", currentDate) // 📌 Yangi sanani saqlash
            editor.apply()
        }

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                editor.putLong("lastScreenOnTime", currentTime)
                editor.putBoolean("isScreenOn", true) // ✅ UI signal olish uchun
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
                    editor.putBoolean("isScreenOn", false) // ✅ UI signal olish uchun
                    editor.commit()

                    Log.d("ScreenTimeReceiver", "✅ SCREEN OFF - Added time: $screenOnDuration, Total time: $newTotalScreenTime")
                } else {
                    Log.e("ScreenTimeReceiver", "❌ ERROR: lastScreenOnTime noto‘g‘ri (${lastScreenOnTime}), vaqt saqlanmaydi.")
                }
            }
        }
    }

    // 📌 Bugungi sanani olish funksiyasi
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
