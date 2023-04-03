package com.example.sportapp

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sportapp.databinding.ActivityMainBinding
import com.example.sportapp.databinding.WebviewLayoutBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.util.*
    
class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var webBinding: WebviewLayoutBinding
    private var link = EMPTY_LINK
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val mSettings = this.getPreferences(Context.MODE_PRIVATE)
        if (mSettings?.contains(LINK_KEY) == true) {
            link = mSettings.getString(LINK_KEY, "").toString()
        }
        //todo СДЕЛАТЬ ЧТОБЫ ПРИ СМЕНЕ ОРИЕНТАЦИИ САЙТ НЕ ПЕРЕЗАГРУЖАЛСЯ
        binding = ActivityMainBinding.inflate(layoutInflater)
        webBinding = WebviewLayoutBinding.inflate(layoutInflater)
        webView = webBinding.webView

        if (!isDeviceOnline(this)) {
            val intent = Intent(this, ErrorActivity::class.java)
            startActivity(intent)
        }
        //todo refactor later
        if (link != EMPTY_LINK) {
            if (isDeviceOnline(this))
                SetupWebView(savedInstanceState, link)
        } else {
            fetchRemoteConfig(savedInstanceState)
        }
    }

    private fun updateQuestion() {
        val questionTextResId = viewModel.currentQuestionText
        binding.questionTextView.setText(questionTextResId)
        if(viewModel.currentIndex == 15){
            binding.trueButton.isEnabled = false
            binding.falseButton.isEnabled = false
            binding.trueButton.visibility = View.INVISIBLE
            binding.falseButton.visibility = View.INVISIBLE
        }
    }

    private fun checkAnswer(userAnswer: Boolean) {
        val correctAnswer = viewModel.currentQuestionAnswer
        var messageResId = R.string.incorrect_toast
        if (userAnswer == correctAnswer) {
            messageResId = R.string.correct_toast
            viewModel.currentPoint++
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun parseBundle(savedInstanceState: Bundle?) {
        val currentIndex = savedInstanceState?.getInt(KEY_INDEX, 0) ?: 0
        viewModel.currentIndex = currentIndex
        val currentPoint = savedInstanceState?.getInt(KEY_INDEX, 0) ?: 0
        viewModel.currentIndex = currentPoint

        //todo maybe do refactor
        binding.score.text = "High Score: ${viewModel.getScore()}%"
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_INDEX, viewModel.currentIndex)
        outState.putInt(KEY_POINT, viewModel.currentPoint)
        outState.putString(LINK_KEY, link)
        webView.saveState(outState);
    }

    private fun isDeviceOnline(context: Context): Boolean {
        val connManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities =
                connManager.getNetworkCapabilities(connManager.activeNetwork)
            return networkCapabilities != null
        } else {
            // below Marshmallow
            val activeNetwork = connManager.activeNetworkInfo
            return activeNetwork?.isConnectedOrConnecting == true && activeNetwork.isAvailable
        }
    }

    private fun fetchRemoteConfig(savedInstanceState: Bundle?): String {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 200
        }
        var fetchedLink = ""
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.key_default_values)
        //todo trycatch
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Config params updated: $updated")
                    Toast.makeText(this, "Fetch and activate succeeded",
                        Toast.LENGTH_SHORT).show()
                    fetchedLink = remoteConfig.getString("KEY_LINK")
                    //todo true replace with checkIsEmu
                    if (fetchedLink == EMPTY_LINK || true) {
                        parseBundle(savedInstanceState)
                        setupQuiz()
                    } else {
                        val sharedPref = this?.getPreferences(Context.MODE_PRIVATE)
                        if (sharedPref != null) {
                            with(sharedPref.edit()) {
                                putString(LINK_KEY, fetchedLink)
                                apply()
                            }
                        }
                        SetupWebView(savedInstanceState, fetchedLink)
                    }
                } else {
                    Toast.makeText(this, "Fetch failed",
                        Toast.LENGTH_SHORT).show()
                }
            }
        return fetchedLink
    }

    private fun SetupWebView(savedInstanceState: Bundle?, urlLink: String) {
        val view = webBinding.root
        setContentView(view)
        webView.webViewClient = WebViewClient()
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        if (savedInstanceState != null)
            webView.restoreState(savedInstanceState)
        else
            webView.loadUrl(urlLink)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        //todo maybe неправильно
        val mWebSettings = webBinding.webView.settings
        mWebSettings.javaScriptEnabled = true
        mWebSettings.loadWithOverviewMode = true
        mWebSettings.useWideViewPort = true
        mWebSettings.domStorageEnabled = true
        mWebSettings.databaseEnabled = true
        mWebSettings.setSupportZoom(false)
        mWebSettings.allowFileAccess = true
        mWebSettings.allowContentAccess = true
        mWebSettings.loadWithOverviewMode = true
        mWebSettings.useWideViewPort = true
        //Сохранение кэша
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT



    }

    private fun checkIsEmu(): Boolean {
       //todo вернуть на место
        if (BuildConfig.DEBUG)
            return false
        val phoneModel = Build.MODEL
        val buildProduct = Build.PRODUCT
        val buildHardware = Build.HARDWARE
        var brand = Build.BRAND
        var result = (Build.FINGERPRINT.startsWith("generic")
                || phoneModel.contains("google_sdk")
                || phoneModel.lowercase(Locale.getDefault()).contains("droid4x")
                || phoneModel.contains("Emulator")
                || phoneModel.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || buildHardware == "goldfish"
                || Build.BRAND.contains("google")
                || buildHardware == "vbox86"
                || buildProduct == "sdk"
                || buildProduct == "google_sdk"
                || buildProduct == "sdk_x86"
                || buildProduct == "vbox86p"
                || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.getDefault()).contains("nox")
                || buildHardware.lowercase(Locale.getDefault()).contains("nox")
                || buildProduct.lowercase(Locale.getDefault()).contains("nox"))

        if (result)
            return true
        result = result or (Build.BRAND.startsWith("generic") &&
                Build.DEVICE.startsWith("generic"))
        if (result)
            return true
        result = result or ("google_sdk" == buildProduct)
        return result
    }

    private fun setupQuiz() {
        val view = binding.root
        setContentView(view)
        with(binding) {
            trueButton.setOnClickListener {
                checkAnswer(true)
                score.text = "High Score: ${viewModel.getScore()}%"
                viewModel.moveToNext()
                updateQuestion()
            }
            falseButton.setOnClickListener {
                checkAnswer(false)
                score.text = "High Score: ${viewModel.getScore()}%"
                viewModel.moveToNext()
                updateQuestion()

            }
        }
        updateQuestion()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {

        }
    }


    companion object {
        const val APP_PREFERENCES = "settings"
        const val KEY_INDEX = "index"
        const val KEY_POINT = "point"
        const val LINK_KEY = "link"
        const val EMPTY_LINK = ""
    }
}