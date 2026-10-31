package com.example.data.sync

import android.content.Context
import com.example.R
import com.example.data.NoteEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(private val db: FirebaseFirestore) {

    // Primary constructor taking injected db; secondary constructor for production runtime
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun currentUserName(): String? = auth.currentUser?.displayName
    fun currentUserEmail(): String? = auth.currentUser?.email
    fun currentUserPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()

    /**
     * Observes real-time note updates in the authenticated user's private cloud collection
     */
    fun observeCloudNotes(): Flow<List<FirestoreNote>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notes"
        val collection = db.collection("users").document(uid).collection("notes")

        collection.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreNote::class.java)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    /**
     * Fetches all cloud notes for the authenticated user once
     */
    suspend fun getCloudNotes(): Result<List<FirestoreNote>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/notes"
        return try {
            val snapshot = db.collection("users").document(uid).collection("notes").get().await()
            val list = snapshot.documents.mapNotNull { it.toObject(FirestoreNote::class.java) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates a local note to Firestore
     */
    suspend fun uploadNote(note: NoteEntity): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "note_${note.id}"
        val docRef = db.collection("users").document(uid).collection("notes").document(docId)

        return try {
            // Check if document already exists to preserve createdAt
            val existingDoc = docRef.get().await()
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "title" to note.title,
                "content" to note.content,
                "emoji" to note.emoji,
                "colorHex" to note.colorHex,
                "isPinned" to note.isPinned,
                "isArchived" to note.isArchived,
                "isTrashed" to note.isTrashed,
                "tags" to note.tags,
                "folder" to note.folder,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (note.bannerColor != null) {
                payload["bannerColor"] = note.bannerColor
            }

            if (!existingDoc.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload.filterValues { it != null }).await()
            } else {
                payload["createdAt"] = existingDoc.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
                docRef.set(payload.filterValues { it != null }, SetOptions.merge()).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a note in Firestore
     */
    suspend fun deleteNote(localNoteId: Long): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "note_$localNoteId"
        val docRef = db.collection("users").document(uid).collection("notes").document(docId)

        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
