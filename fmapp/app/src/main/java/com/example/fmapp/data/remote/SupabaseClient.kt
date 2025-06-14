package com.example.fmapp.data.remote

import io.supabase.common.SupabaseClient as SupabaseClientInstance // Renamed to avoid conflict
import io.supabase.gotrue.GoTrue
import io.supabase.postgrest.Postgrest
import io.supabase.storage.Storage

// Placeholder for Supabase credentials.
// In a real app, these would come from a secure source (e.g., buildConfig or a gradle file not checked into VCS).
// For this tool, we use placeholders that are syntactically valid.
const val TMP_SUPABASE_URL = "https://placeholder.supabase.co" // Syntactically valid placeholder
const val TMP_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBsYWNlaG9sZGVyIiwicm9sZSI6ImFub24iLCJpYXQiOjE2NzgwMzYwMDAsImV4cCI6MTk5MzYxMjAwMH0.completely_fake_but_valid_looking_jwt_token" // Syntactically valid placeholder

object AppSupabaseClient {

    private val supabaseClient = SupabaseClientInstance( // Use the renamed import
        supabaseUrl = TMP_SUPABASE_URL,
        supabaseKey = TMP_SUPABASE_ANON_KEY
    )

    val auth: GoTrue = supabaseClient.auth
    val database: Postgrest = supabaseClient.database
    const val BUCKET_TRANSACTION_FILES = "transaction_files" // Example bucket
    val storageTransactionFiles: Storage = supabaseClient.storage(BUCKET_TRANSACTION_FILES)
}
