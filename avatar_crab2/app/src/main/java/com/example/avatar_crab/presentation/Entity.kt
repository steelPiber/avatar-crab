import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_data")
data class ActivityData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: String,
    val tag: String,
    val timestamp: String
)
