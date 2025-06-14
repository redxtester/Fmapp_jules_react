package com.example.fmapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sim_cards",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class SimCard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val simNickname: String,
    val telecomProvider: String,
    val officialRegisteredName: String? = null,
    val userId: String // To associate SIM with a user (from Supabase)
)
