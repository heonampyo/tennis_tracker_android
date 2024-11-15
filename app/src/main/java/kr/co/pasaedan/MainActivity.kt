package kr.co.pasaedan

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kr.co.pasaedan.ui.theme.PasaedanProjectTheme

class MainActivity : ComponentActivity() {

    val permissionString = "android.permission.POST_NOTIFICATIONS"
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getFirebaseToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        enableEdgeToEdge()
        setContent {
            PasaedanProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    permissionString
                ) == PackageManager.PERMISSION_GRANTED -> {
                    getFirebaseToken()
                }
                shouldShowRequestPermissionRationale(permissionString) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(permissionString)
                }
            }
        } else {
            getFirebaseToken()
        }
    }

    private fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "토큰 가져오기 실패", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM", "토큰: $token")
                sendRegistrationTokenToServer(token)
            }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("알림 권한 필요")
            .setMessage("앱 알림을 받으려면 알림 권한이 필요합니다.")
            .setPositiveButton("권한 요청") { _, _ ->
                requestPermissionLauncher.launch(permissionString)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun sendRegistrationTokenToServer(token: String) {
        Toast.makeText(this, "token : $token", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var webView: WebView? = null
    val state = rememberPullToRefreshState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) { // 아래로 당길 때
                    state.startRefresh()
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
            .nestedScroll(nestedScrollConnection) // 추가된 코드
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this

                    // 스크롤 리스너 설정
                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        if (scrollY == 0) {
                            setOverScrollMode(View.OVER_SCROLL_NEVER)
                        } else {
                            setOverScrollMode(View.OVER_SCROLL_ALWAYS)
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onJsAlert(
                            view: WebView,
                            url: String,
                            message: String,
                            result: JsResult
                        ): Boolean {
                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton("확인") { _, _ ->
                                    result.confirm()
                                }
                                .setOnCancelListener {
                                    result.cancel()
                                }
                                .create()
                                .show()
                            return true
                        }

                        override fun onJsConfirm(
                            view: WebView,
                            url: String,
                            message: String,
                            result: JsResult
                        ): Boolean {
                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton("확인") { _, _ ->
                                    result.confirm()
                                }
                                .setNegativeButton("취소") { _, _ ->
                                    result.cancel()
                                }
                                .setOnCancelListener {
                                    result.cancel()
                                }
                                .create()
                                .show()
                            return true
                        }

                        override fun onJsPrompt(
                            view: WebView,
                            url: String,
                            message: String,
                            defaultValue: String?,
                            result: JsPromptResult
                        ): Boolean {
                            val input = EditText(context)
                            input.setText(defaultValue)

                            AlertDialog.Builder(context)
                                .setMessage(message)
                                .setView(input)
                                .setPositiveButton("확인") { _, _ ->
                                    result.confirm(input.text.toString())
                                }
                                .setNegativeButton("취소") { _, _ ->
                                    result.cancel()
                                }
                                .setOnCancelListener {
                                    result.cancel()
                                }
                                .create()
                                .show()
                            return true
                        }
                    }

                    loadUrl("https://tennis-tracker-azure.vercel.app/")
                }
            }
        )

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            webView?.reload()
            delay(1500)
            state.endRefresh()
        }
    }
    val context = LocalContext.current
    BackHandler {
        webView?.let {
            if(it.canGoBack()){
                it.goBack()
            }else{
                (context as? Activity)?.finishAndRemoveTask()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PasaedanProjectTheme {
        Greeting("Android")
    }
}
