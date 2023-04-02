package com.example.sportapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sportapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (!isDeviceOnline(this)){
            val intent = Intent(this,ErrorActivity::class.java)
            startActivity(intent)
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        parseBundle(savedInstanceState)

        with(binding) {
            trueButton.setOnClickListener {
                checkAnswer(true)
                score.text = "High Score: ${viewModel.getScore()}%"
                viewModel.moveToNext()
                updateQuestion()
            }
            falseButton.setOnClickListener {
                checkAnswer(true)
                score.text = "High Score: ${viewModel.getScore()}%"
                viewModel.moveToNext()
                updateQuestion()

            }
            updateQuestion()

        }
    }

    private fun updateQuestion() {
        val questionTextResId = viewModel.currentQuestionText
        binding.questionTextView.setText(questionTextResId)
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
    }

    private fun isDeviceOnline(context: Context): Boolean {
        val connManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
            if (networkCapabilities == null) {
                return false
            } else {
                return true
            }
        } else {
            // below Marshmallow
            val activeNetwork = connManager.activeNetworkInfo
            if (activeNetwork?.isConnectedOrConnecting == true && activeNetwork.isAvailable) {
                return true
            } else {
                return false
            }
        }
    }

    companion object {
        const val KEY_INDEX = "index"
        const val KEY_POINT = "point"
    }
}