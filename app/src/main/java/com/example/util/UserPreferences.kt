package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitprogress_user_prefs", Context.MODE_PRIVATE)

    private val _userBodyweightKg = MutableStateFlow(getUserBodyweightKg())
    val userBodyweightKg: StateFlow<Float> = _userBodyweightKg.asStateFlow()

    fun getUserBodyweightKg(): Float {
        return prefs.getFloat(KEY_BODYWEIGHT, 75.0f)
    }

    fun setUserBodyweightKg(weightKg: Float) {
        val validWeight = if (weightKg <= 0f) 75.0f else weightKg
        prefs.edit().putFloat(KEY_BODYWEIGHT, validWeight).apply()
        _userBodyweightKg.value = validWeight
    }

    companion object {
        private const val KEY_BODYWEIGHT = "user_bodyweight_kg"
    }
}
