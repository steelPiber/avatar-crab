package com.example.avatar_crab.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.userinfo.UserInfoActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign-in button click listener
        findViewById<com.google.android.gms.common.SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
    }

    // Initiating the Google Sign-In process
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    // Handle the result of the Google Sign-In process
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle the Google Sign-In result
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email
                // Check if the email exists in the Userinfo table
                if (email != null) {
                    checkIfUserInfoExists(email)
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if the user's info exists in the Userinfo table
    private fun checkIfUserInfoExists(email: String) {
        RetrofitClient.recordsInstance.checkUserInfo(email).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    // If user info exists, navigate to MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If user info does not exist, navigate to UserInfoActivity for data collection
                    val intent = Intent(this@LoginActivity, UserInfoActivity::class.java)
                    intent.putExtra("email", email)  // Pass the user's email to the next activity
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Sign out (optional feature if needed)
    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
