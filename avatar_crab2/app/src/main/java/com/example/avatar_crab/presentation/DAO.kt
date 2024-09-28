import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activityData: ActivityData)

    @Query("SELECT * FROM activity_data")
    suspend fun getAllActivityData(): List<ActivityData>

    @Query("DELETE FROM activity_data")
    suspend fun deleteAll()
}
