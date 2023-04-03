package com.example.sportapp

import androidx.lifecycle.ViewModel

class MainViewModel:ViewModel() {
    var currentPoint = 0
    var textScore = 0
    var currentIndex = 0
    private val questionBank = listOf(
        Question(R.string.question_1,true),
        Question(R.string.question_2,false),
        Question(R.string.question_3,false),
        Question(R.string.question_4,true),
        Question(R.string.question_5,true),
        Question(R.string.question_6,true),
        Question(R.string.question_7,false),
        Question(R.string.question_8,true),
        Question(R.string.question_9,false),
        Question(R.string.question_10,false),
        Question(R.string.question_11,false),
        Question(R.string.question_12,false),
        Question(R.string.question_13,true),
        Question(R.string.question_14,true),
        Question(R.string.question_15,true),
        Question(R.string.ending,true))
    val currentQuestionAnswer:Boolean
        get() = questionBank[currentIndex].answer
    val currentQuestionText:Int
        get() = questionBank[currentIndex].textResId

    fun getScore()= 100*currentPoint/questionBank.size

    fun moveToNext(){
        currentIndex = (currentIndex + 1) % questionBank.size
    }

}