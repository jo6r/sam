package xyz.jo6r.sam.sammeal2

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class MealRequest(
    val id: String,
    val strava_program: String,
    val den: String
)

data class MealResponse(
    val success: Boolean,
    val error: String?,
    val data: MealData?
)

data class MealData(
    val qrcode: String?,
    val meal_program: String?,
    val day: String?,
    val paid: Boolean?,
    val already_served: Boolean? = null,
    val message: String? = null,
    val total_served_meals_all_users: Int? = null,
    val total_ordered_meals_all_users: Int? = null
)

interface MealApi {
    @POST("meals")
    suspend fun verifyMeal(@Body request: MealRequest): MealResponse

    @GET("stats")
    suspend fun getStats(
        @Query("query") query: String,
        @Query("strava_program") meal: String,
        @Query("den") day: String
    ): MealResponse

    @DELETE("meals")
    suspend fun resetMeals(): MealResponse
}
