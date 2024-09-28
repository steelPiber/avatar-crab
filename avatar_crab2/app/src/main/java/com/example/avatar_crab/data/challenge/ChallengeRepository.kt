package com.example.avatar_crab.data.challenge

import androidx.lifecycle.LiveData
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.exercise.ExerciseRecordDao
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ChallengeRepository(
    private val challengeDao: ChallengeDao,
    private val exerciseRecordDao: ExerciseRecordDao
) {

    suspend fun insertChallenge(challenge: Challenge) {
        challengeDao.insertChallenge(challenge)
    }

    suspend fun insertChallenges(challenges: List<Challenge>) {
        challengeDao.insertChallenges(challenges)
    }

    fun getChallengesByDate(date: Long): LiveData<List<Challenge>> {
        return challengeDao.getChallengesByDate(date)
    }

    suspend fun deleteChallengesByDate(date: Long) {
        challengeDao.deleteChallengesByDate(date)
    }

    fun getRandomChallenges(date: Long): List<Challenge> {
        val challengeList = listOf(
            Challenge(description = "운동 1", details = "하루 동안 운동을 5% 이상 채우세요.", date = date, target = 5, month = getCurrentMonth()),
            Challenge(description = "운동 2", details = "하루 동안 활동을 20% 이상 채우세요.", date = date, target = 20, month = getCurrentMonth()),
            Challenge(description = "운동 3", details = "하루 동안 1km를 걷거나 뛰세요.", date = date, target = 1000, month = getCurrentMonth()),
            Challenge(description = "운동 4", details = "하루 동안 2km를 걷거나 뛰세요.", date = date, target = 2000, month = getCurrentMonth()),
            Challenge(description = "운동 5", details = "하루 동안 3km를 걷거나 뛰세요.", date = date, target = 3000, month = getCurrentMonth()),
            Challenge(description = "운동 6", details = "하루 동안 4km를 걷거나 뛰세요.", date = date, target = 4000, month = getCurrentMonth()),
            Challenge(description = "운동 7", details = "하루 동안 5km를 걷거나 뛰세요.", date = date, target = 5000, month = getCurrentMonth()),
            Challenge(description = "운동 8", details = "하루 동안 운동으로 100 칼로리를 소모하세요.", date = date, target = 100, month = getCurrentMonth()),
            Challenge(description = "운동 9", details = "하루 동안 운동을 3% 이상 채우세요.", date = date, target = 3, month = getCurrentMonth()),
            Challenge(description = "운동 10", details = "하루 동안 활동을 10% 이상 채우세요.", date = date, target = 10, month = getCurrentMonth()),
            Challenge(description = "운동 11", details = "하루 동안 활동을 15% 이상 채우세요.", date = date, target = 15, month = getCurrentMonth()),
            Challenge(description = "운동 12", details = "하루 동안 운동으로 50 칼로리를 소모하세요.", date = date, target = 50, month = getCurrentMonth()),
            Challenge(description = "운동 13", details = "하루 동안 운동으로 150 칼로리를 소모하세요.", date = date, target = 150, month = getCurrentMonth()),
            Challenge(description = "운동 14", details = "하루 동안 운동으로 200 칼로리를 소모하세요.", date = date, target = 200, month = getCurrentMonth()),
            Challenge(description = "운동 15", details = "하루 동안 운동으로 250 칼로리를 소모하세요.", date = date, target = 250, month = getCurrentMonth()),
            Challenge(description = "운동 16", details = "하루 동안 운동으로 300 칼로리를 소모하세요.", date = date, target = 300, month = getCurrentMonth()),
            Challenge(description = "운동 17", details = "하루 동안 운동으로 350 칼로리를 소모하세요.", date = date, target = 350, month = getCurrentMonth()),
        )
        return challengeList.shuffled().take(3)
    }

    suspend fun getOrGenerateChallenges(date: Long): List<Challenge> {
        val existingChallenges = withContext(Dispatchers.IO) {
            challengeDao.getChallengesByDateSync(date)
        }

        return if (existingChallenges.isNotEmpty()) {
            existingChallenges.take(3)
        } else {
            val newChallenges = getRandomChallenges(date)
            insertChallenges(newChallenges)
            newChallenges
        }
    }

    suspend fun updateChallengesWithActivityData(activityData: List<ActivityData>, exerciseRecords: List<ExerciseRecordEntity>, calendar: Calendar) {
        val todayDate = getTodayDate()
        val startOfDay = getStartOfDayTimestamp()
        val endOfDay = getEndOfDayTimestamp()

        // 오늘 날짜에 해당하는 활동 데이터만 필터링
        val todayActivityData = activityData.filter { it.timestamp in startOfDay..endOfDay }

        // 특정 활동 태그별로 시간(분) 계산
        val activeTime = todayActivityData.count { it.tag == "active" }
        val exerciseTime = todayActivityData.count { it.tag == "exercise" }
        val totalMinutes = todayActivityData.size

        val challenges = withContext(Dispatchers.IO) {
            challengeDao.getChallengesByDateSync(todayDate)
        }

        var allChallengesCompleted = true

        challenges.forEach { challenge ->
            when (challenge.description) {
                "운동 1", "운동 9" -> {
                    challenge.progress = if (totalMinutes > 0) (exerciseTime.toFloat() / totalMinutes * 100).toInt() else 0
                }
                "운동 2", "운동 10", "운동 11" -> {
                    challenge.progress = if (totalMinutes > 0) (activeTime.toFloat() / totalMinutes * 100).toInt() else 0
                }
                "운동 3", "운동 4", "운동 5", "운동 6", "운동 7" -> {
                    val totalDistance = exerciseRecords.sumOf { it.distance }
                    challenge.progress = totalDistance.toInt()
                }
                "운동 12", "운동 8", "운동 13", "운동 14", "운동 15", "운동 16", "운동 17" -> {
                    val totalCalories = exerciseRecords.sumOf { it.calories }
                    challenge.progress = totalCalories.toInt()
                }
            }
            if (challenge.progress < challenge.target) {
                allChallengesCompleted = false
            }
        }

        withContext(Dispatchers.IO) {
            challengeDao.insertChallenges(challenges)
        }

        // 모든 챌린지가 완료된 경우 캘린더에 별 스탬프 찍기
        if (allChallengesCompleted) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            // 캘린더에 별 스탬프를 찍는 로직 추가 (예: 별 스탬프를 기록하는 DB에 추가)
        }
    }

    private fun getTodayDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    private fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
    }
}
