package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutRoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
        MonthlyAnalysisEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun monthlyAnalysisDao(): MonthlyAnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitprogress_db"
                )
                    .addCallback(SeedDatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                seedInitialData(database)
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            val exerciseDao = db.exerciseDao()
            val routineDao = db.workoutRoutineDao()

            // 1. Seed Exercises
            val defaultExercises = listOf(
                ExerciseEntity(name = "Développé Couché", category = "Pectoraux", equipment = "Barre", notes = "Exercice polyarticulaire roi pour la poitrine"),
                ExerciseEntity(name = "Développé Incliné", category = "Pectoraux", equipment = "Haltères", notes = "Cible le haut des pectoraux"),
                ExerciseEntity(name = "Écarté à la Poulie", category = "Pectoraux", equipment = "Poulie", notes = "Isolation pour la finition"),
                ExerciseEntity(name = "Squat Barre", category = "Jambes", equipment = "Barre", notes = "Cible quadriceps et fessiers"),
                ExerciseEntity(name = "Presse à Cuisses", category = "Jambes", equipment = "Machine", notes = "Volume et sécurité du bas du dos"),
                ExerciseEntity(name = "Soulevé de Terre Jambes Tendues", category = "Jambes", equipment = "Barre", notes = "Ischio-jambiers et fessiers"),
                ExerciseEntity(name = "Soulevé de Terre", category = "Dos", equipment = "Barre", notes = "Force globale et chaîne postérieure"),
                ExerciseEntity(name = "Tractions Prise Pronation", category = "Dos", equipment = "Poids du corps", notes = "Largeur du dos (Grand Dorsal)"),
                ExerciseEntity(name = "Rowing Barre", category = "Dos", equipment = "Barre", notes = "Épaisseur du dos"),
                ExerciseEntity(name = "Développé Militaire", category = "Épaules", equipment = "Barre", notes = "Deltoïdes antérieurs et triceps"),
                ExerciseEntity(name = "Élévations Latérales", category = "Épaules", equipment = "Haltères", notes = "Deltoïdes latéraux pour la largeur"),
                ExerciseEntity(name = "Curl Biceps Barre EZ", category = "Bras", equipment = "Barre", notes = "Masse des biceps"),
                ExerciseEntity(name = "Extension Triceps Poulie", category = "Bras", equipment = "Poulie", notes = "Isolation des triceps"),
                ExerciseEntity(name = "Crunchs suspendus", category = "Abdos", equipment = "Poids du corps", notes = "Grand droit et abdominaux")
            )

            exerciseDao.insertAllExercises(defaultExercises)

            // Get inserted exercise ids
            val allEx = exerciseDao.getExerciseById(1) // Ensure DB ready
            val exList = listOf(
                1L to defaultExercises[0], // Dev Couché
                2L to defaultExercises[1], // Dev Incliné
                3L to defaultExercises[2], // Écarté Poulie
                4L to defaultExercises[3], // Squat
                5L to defaultExercises[4], // Presse
                6L to defaultExercises[5], // SDT JT
                7L to defaultExercises[6], // SDT
                8L to defaultExercises[7], // Tractions
                9L to defaultExercises[8], // Rowing
                10L to defaultExercises[9], // Dev Militaire
                11L to defaultExercises[10], // Elev Lat
                12L to defaultExercises[11], // Curl Biceps
                13L to defaultExercises[12]  // Ext Triceps
            )

            // 2. Seed Default Workout Routine: "Séance Push (Pectoraux, Épaules, Triceps)"
            val pushRoutineId = routineDao.insertRoutine(
                WorkoutRoutineEntity(
                    title = "Séance Push (Pec / Épaules / Triceps)",
                    description = "Séance axée sur les mouvements de poussée. Idéal pour hypertrophie et force.",
                    reminderTime = "18:00",
                    isReminderEnabled = false
                )
            )

            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pushRoutineId, exerciseId = 1L, targetSets = 4, targetReps = 8, targetWeightKg = 70f, restTimeSeconds = 120, orderIndex = 0))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pushRoutineId, exerciseId = 2L, targetSets = 3, targetReps = 10, targetWeightKg = 24f, restTimeSeconds = 90, orderIndex = 1))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pushRoutineId, exerciseId = 10L, targetSets = 3, targetReps = 10, targetWeightKg = 40f, restTimeSeconds = 90, orderIndex = 2))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pushRoutineId, exerciseId = 11L, targetSets = 4, targetReps = 12, targetWeightKg = 10f, restTimeSeconds = 60, orderIndex = 3))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pushRoutineId, exerciseId = 13L, targetSets = 3, targetReps = 12, targetWeightKg = 25f, restTimeSeconds = 60, orderIndex = 4))

            // Seed Default Workout Routine: "Séance Pull (Dos & Biceps)"
            val pullRoutineId = routineDao.insertRoutine(
                WorkoutRoutineEntity(
                    title = "Séance Pull (Dos / Biceps)",
                    description = "Séance axée sur les tirages pour développer un dos large et puissant.",
                    reminderTime = "18:00",
                    isReminderEnabled = false
                )
            )

            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pullRoutineId, exerciseId = 8L, targetSets = 4, targetReps = 8, targetWeightKg = 0f, restTimeSeconds = 120, orderIndex = 0))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pullRoutineId, exerciseId = 9L, targetSets = 4, targetReps = 10, targetWeightKg = 60f, restTimeSeconds = 90, orderIndex = 1))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = pullRoutineId, exerciseId = 12L, targetSets = 3, targetReps = 10, targetWeightKg = 30f, restTimeSeconds = 75, orderIndex = 2))

            // Seed Default Workout Routine: "Séance Legs (Cuisses & Mollets)"
            val legRoutineId = routineDao.insertRoutine(
                WorkoutRoutineEntity(
                    title = "Séance Legs (Jambes & Fessiers)",
                    description = "Séance complète pour le bas du corps.",
                    reminderTime = "18:00",
                    isReminderEnabled = false
                )
            )

            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = legRoutineId, exerciseId = 4L, targetSets = 4, targetReps = 8, targetWeightKg = 90f, restTimeSeconds = 150, orderIndex = 0))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = legRoutineId, exerciseId = 5L, targetSets = 3, targetReps = 12, targetWeightKg = 140f, restTimeSeconds = 90, orderIndex = 1))
            routineDao.insertRoutineExercise(RoutineExerciseEntity(routineId = legRoutineId, exerciseId = 6L, targetSets = 3, targetReps = 10, targetWeightKg = 65f, restTimeSeconds = 90, orderIndex = 2))
        }
    }
}
