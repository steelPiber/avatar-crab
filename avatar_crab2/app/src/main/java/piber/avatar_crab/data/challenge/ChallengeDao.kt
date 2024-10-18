package piber.avatar_crab.data.challenge

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE date = :date")
    fun getChallengesByDate(date: Long): LiveData<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE date = :date")
    fun getChallengesByDateSync(date: Long): List<Challenge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: Challenge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<Challenge>)

    @Query("DELETE FROM challenges WHERE date = :date")
    suspend fun deleteChallengesByDate(date: Long)

    @Update
    suspend fun updateChallenges(challenges: List<Challenge>)
}
