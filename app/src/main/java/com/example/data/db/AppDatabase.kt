package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.NutritionDao
import com.example.data.entity.LoggedMealEntity
import com.example.data.entity.MealAlternativeEntity
import com.example.data.entity.MealSlotEntity
import com.example.data.entity.PlanEntity

@Database(
    entities = [
        PlanEntity::class,
        MealSlotEntity::class,
        MealAlternativeEntity::class,
        LoggedMealEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrition_planner_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
