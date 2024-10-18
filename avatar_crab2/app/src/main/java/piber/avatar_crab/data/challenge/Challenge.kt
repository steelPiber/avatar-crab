package piber.avatar_crab.data.challenge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val details: String,
    val date: Long,
    val target: Int,
    var progress: Int = 0,
    val month: Int
)
