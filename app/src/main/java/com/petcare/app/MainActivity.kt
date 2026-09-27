package com.petcare.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.petcare.app.ui.navigation.AppNavHost
import com.petcare.app.ui.navigation.Routes
import com.petcare.app.ui.theme.PetCareTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** แท็บที่ต้องเปิดเมื่อแตะ Notification */
    private var openTab by mutableStateOf<String?>(null)

    // บังคับภาษาไทยให้ส่วนประกอบของระบบ (DatePicker, TimePicker) แสดงเป็นภาษาไทยด้วย
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(THAI) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // แถบสถานะอยู่บนพื้นสีเขียวมรกตเข้มเสมอ จึงใช้ไอคอนสีขาว
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        openTab = tabFrom(intent)

        setContent {
            PetCareTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AppNavHost(openTab = openTab, onTabOpened = { openTab = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTab = tabFrom(intent)
    }

    private fun tabFrom(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_OPEN_TAB)?.also { intent.removeExtra(EXTRA_OPEN_TAB) }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_NOTIFICATIONS = Routes.NOTIFICATIONS
        private val THAI: Locale = Locale.forLanguageTag("th-TH")
    }
}
