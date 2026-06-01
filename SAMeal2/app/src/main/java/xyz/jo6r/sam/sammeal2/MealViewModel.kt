package xyz.jo6r.sam.sammeal2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar

data class UIState(
    val selectedDay: String = "ct",
    val selectedMeal: String = "snidane",
    val scannedQrCode: String? = null,
    val verificationResult: MealResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalServed: Int = 0,
    val totalOrdered: Int = 0
)

class MealViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer FB2c00KYKfwfk7nBvUaK")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.samorlova.cz/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MealApi::class.java)

    init {
        preselectDayAndMeal()
        fetchStats()
    }

    private fun preselectDayAndMeal() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) // 0-based
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Preselect day based on date
        val day = when {
            dayOfMonth == 13 && month == Calendar.AUGUST -> "ct"
            dayOfMonth == 14 && month == Calendar.AUGUST -> "pa"
            dayOfMonth == 15 && month == Calendar.AUGUST -> "so"
            dayOfMonth == 16 && month == Calendar.AUGUST -> "ne"
            else -> "ct" // Default
        }

        // Preselect meal based on time
        val meal = when {
            hour < 10 -> "snidane"
            hour < 15 -> "obed"
            else -> "vecere"
        }

        _uiState.value = _uiState.value.copy(selectedDay = day, selectedMeal = meal)
    }

    fun fetchStats() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                // Optimalizace: spuštění obou požadavků paralelně
                val servedDeferred = async { api.getStats("total_served_all_users", state.selectedMeal, state.selectedDay) }
                val orderedDeferred = async { api.getStats("total_ordered_all_users", state.selectedMeal, state.selectedDay) }
                
                val servedResponse = servedDeferred.await()
                val orderedResponse = orderedDeferred.await()
                
                _uiState.value = _uiState.value.copy(
                    totalServed = servedResponse.data?.total_served_meals_all_users ?: 0,
                    totalOrdered = orderedResponse.data?.total_ordered_meals_all_users ?: 0
                )
            } catch (e: Exception) {
                // Silently fail stats or log
            }
        }
    }

    fun selectDay(day: String) {
        _uiState.value = _uiState.value.copy(selectedDay = day, verificationResult = null)
        fetchStats()
    }

    fun selectMeal(meal: String) {
        _uiState.value = _uiState.value.copy(selectedMeal = meal, verificationResult = null)
        fetchStats()
    }

    fun onQrCodeScanned(qrCode: String) {
        if (_uiState.value.scannedQrCode != qrCode) {
            _uiState.value = _uiState.value.copy(scannedQrCode = qrCode, verificationResult = null)
        }
    }

    fun verifyQrCode() {
        val state = _uiState.value
        val qrCode = state.scannedQrCode ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val request = MealRequest(
                    id = qrCode,
                    strava_program = state.selectedMeal,
                    den = state.selectedDay
                )
                val response = api.verifyMeal(request)
                _uiState.value = _uiState.value.copy(verificationResult = response, isLoading = false)
                fetchStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun resetAllMeals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                api.resetMeals()
                fetchStats()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verificationResult = null,
                    scannedQrCode = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
