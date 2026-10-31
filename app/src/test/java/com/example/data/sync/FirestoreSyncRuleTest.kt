package com.example.data.sync

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.NoteEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun uploadNote_authenticatedUser_createsDocumentSuccessfully() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreSyncRepository(firestore)

        val note = NoteEntity(
            id = 101L,
            title = "Alice Work Note",
            content = "# Meeting notes\n- [ ] Follow up with team",
            emoji = "📝",
            colorHex = "#202124",
            isPinned = true,
            isArchived = false,
            isTrashed = false,
            tags = "work,projects",
            folder = "Work"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.uploadNote(note)
        }
        assertTrue(result.isSuccess)
        assertEquals("note_101", result.getOrNull())

        val cloudNotes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCloudNotes()
        }
        assertTrue(cloudNotes.isSuccess)
        val uploaded = cloudNotes.getOrNull()?.find { it.title == "Alice Work Note" }
        assertNotNull(uploaded)
        assertEquals(aliceUid, uploaded?.userId)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceNotesDirectly() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSyncRepository(firestore)

        val note = NoteEntity(
            id = 202L,
            title = "Alice Secret",
            content = "Confidential",
            emoji = "🔒",
            colorHex = "#202124"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.uploadNote(note)
        }

        // Now Bob signs in
        signInTestUser(BOB_EMAIL)
        val bobDocRef = firestore.collection("users").document(aliceUid).collection("notes").document("note_202")

        val exception = assertThrows(FirebaseFirestoreException::class.java) {
            runBlocking {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    bobDocRef.get().await()
                }
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    @Test
    fun unauthenticatedUser_cannotAccessNotes() = runBlocking {
        auth.signOut()
        val unauthDocRef = firestore.collection("users").document("any_uid").collection("notes").document("note_303")

        val exception = assertThrows(FirebaseFirestoreException::class.java) {
            runBlocking {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    unauthDocRef.get().await()
                }
            }
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_sync@test.com"
        const val BOB_EMAIL = "bob_sync@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
