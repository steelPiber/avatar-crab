// AppDatabase.kt
package piber.avatar_crab.data


import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import piber.avatar_crab.data.challenge.Challenge
import piber.avatar_crab.data.challenge.ChallengeDao
import piber.avatar_crab.data.exercise.Converters
import piber.avatar_crab.data.exercise.ExerciseRecordDao
import piber.avatar_crab.data.exercise.ExerciseRecordEntity

@Database(entities = [ActivityData::class, ExerciseRecordEntity::class, Challenge::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseRecordDao(): ExerciseRecordDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "avatar_crab_database"
                )
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE `exercise_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `distance` REAL NOT NULL,
                        `elapsedTime` INTEGER NOT NULL,
                        `calories` REAL NOT NULL,
                        `avgPace` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `segments` TEXT NOT NULL,
                        `pathPoints` TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE `challenges` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `description` TEXT NOT NULL,
                        `progress` REAL NOT NULL,
                        `target` REAL NOT NULL,
                        `date` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 필요한 경우 새로운 스키마 변경 사항을 여기에 작성
                // 예: 새로운 컬럼 추가, 기존 테이블 수정 등
                // 여기서는 아무것도 하지 않지만, 필요 시 여기에 SQL 문을 추가
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE activity_data ADD COLUMN calories REAL NOT NULL DEFAULT 0.0
                """)
                database.execSQL("""
                    ALTER TABLE activity_data ADD COLUMN distance REAL NOT NULL DEFAULT 0.0
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) { // 새 마이그레이션 추가
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE exercise_records ADD COLUMN email TEXT NOT NULL DEFAULT ''
                """)
            }
        }
    }
}
