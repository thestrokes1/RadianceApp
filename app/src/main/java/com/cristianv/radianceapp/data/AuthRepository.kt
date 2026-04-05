package com.cristianv.radianceapp.data

import com.cristianv.radianceapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String
    ): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthRepository", "Starting Firebase signUp for: $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Synchronize Auth user to Firestore database
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                fullName = fullName,
                createdAt = System.currentTimeMillis()
            )

            db.collection("users").document(firebaseUser.uid).set(user).await()
            
            android.util.Log.d("AuthRepository", "Firebase signUp and Firestore sync SUCCESS: ${firebaseUser.email}")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Firebase signUp or Firestore sync FAILED: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthRepository", "signInWithGoogle: creating credential")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            android.util.Log.d("AuthRepository", "signInWithGoogle: calling signInWithCredential")
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user!!
            android.util.Log.d("AuthRepository", "signInWithGoogle: SUCCESS — uid=${firebaseUser.uid}, email=${firebaseUser.email}, isNewUser=${result.additionalUserInfo?.isNewUser}")

            // Sync with Firestore if it's a new user
            if (result.additionalUserInfo?.isNewUser == true) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "",
                    createdAt = System.currentTimeMillis()
                )
                db.collection("users").document(firebaseUser.uid).set(user).await()
                android.util.Log.d("AuthRepository", "signInWithGoogle: new user synced to Firestore")
            }

            Result.success(firebaseUser)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "signInWithGoogle: FAILED — ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
