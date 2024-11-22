package kr.co.pasaedan

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.pasaedan.ui.theme.PasaedanProjectTheme

class MainActivity : ComponentActivity() {
    private val fcmViewModel: FcmViewModel by viewModels()

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
        observeViewModel()

        setContent {
            PasaedanProjectTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 기존 컨텐츠
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    // FAB 추가
                    SmallFloatingActionButton(
                        onClick = { showNameInputDialog(isInitial = false) },
                        modifier = Modifier
                            .padding(start = 10.dp, bottom = 80.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Icon(Icons.Filled.Notifications, "이름 수정")
                    }
                }
            }
        }

        // 최초 실행 시 이름 확인
        checkUserName()
    }
    private fun checkUserName() {
        val userName = PreferenceManager.getUserName(this)
        if (userName.isNullOrEmpty()) {
            showNameInputDialog(isInitial = true)
        }
    }

    private fun showNameInputDialog(isInitial: Boolean) {
        // 수직 레이아웃 생성
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        // 이름 입력 EditText
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            privateImeOptions = "nm"
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setText(PreferenceManager.getUserName(this@MainActivity) ?: "")

            doAfterTextChanged { editable ->
                val currentText = editable?.toString() ?: ""
                val filteredText = currentText.replace("\\s".toRegex(), "")

                if (currentText != filteredText) {
                    val selection = selectionStart
                    setText(filteredText)
                    if (selection <= filteredText.length) {
                        setSelection(selection)
                    } else {
                        setSelection(filteredText.length)
                    }
                }
            }
        }

        // 전체 푸시 수신 체크박스
        val checkbox = CheckBox(this).apply {
            text = "전체 푸시 수신"
            isChecked = PreferenceManager.getReceiveAllPush(this@MainActivity) // 저장된 설정 불러오기
            setPadding(0, 16, 0, 0)
        }

        // 컨테이너에 뷰들 추가
        container.addView(input)
        container.addView(checkbox)

        val dialog = AlertDialog.Builder(this)
            .setTitle("이름 입력")
            .setMessage("푸시에 사용되는 이름을 입력해주세요.")
            .setView(container)
            .setPositiveButton("확인") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    PreferenceManager.setUserName(this, name)
                    PreferenceManager.setReceiveAllPush(this, checkbox.isChecked)
                    PreferenceManager.getFcmToken(this)?.let {
                        sendRegistrationTokenToServer(it)
                    }
                } else {
                    showNameInputDialog(isInitial)
                }
            }
            .create()

        if (!isInitial) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "취소") { _, _ -> }
        }

        dialog.setCancelable(!isInitial)
        dialog.show()
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                fcmViewModel.updateState.collect { result ->
                    result?.fold(
                        onSuccess = {}, onFailure = { exception ->
                            Toast.makeText(this@MainActivity, "${exception.message}", Toast.LENGTH_LONG).show()
                        }
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
                if(token == PreferenceManager.getFcmToken(this@MainActivity)){
                    Log.d("FCM", "토큰 같아서 통신 안보냄.")
                    return@addOnCompleteListener
                }

                PreferenceManager.setFcmToken(this@MainActivity, token)
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
        PreferenceManager.getUserName(this)?.let {
            fcmViewModel.updateFcmToken(userId = it, fcmToken = token, isAllPush = PreferenceManager.getReceiveAllPush(this))
        }
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
