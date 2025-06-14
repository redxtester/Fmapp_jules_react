package com.example.fmapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(simCard: SimCard)

    @Update
    suspend fun update(simCard: SimCard)

    @Delete
    suspend fun delete(simCard: SimCard)

    @Query("SELECT * FROM sim_cards WHERE id = :id AND userId = :userId")
    fun getSimCardById(id: Int, userId: String): Flow<SimCard?>

    @Query("SELECT * FROM sim_cards WHERE userId = :userId ORDER BY simNickname ASC")
    fun getAllSimCards(userId: String): Flow<List<SimCard>>

    @Query("SELECT * FROM sim_cards WHERE phoneNumber = :phoneNumber AND userId = :userId")
    fun getSimCardByPhoneNumber(phoneNumber: String, userId: String): Flow<SimCard?>
}
