package piber.avatar_crab.utils

import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class GoogleSignInAccountBuilder {
    private var displayName: String? = null
    private var email: String? = null
    private var photoUrl: Uri? = null

    fun setDisplayName(displayName: String?): GoogleSignInAccountBuilder {
        this.displayName = displayName
        return this
    }

    fun setEmail(email: String?): GoogleSignInAccountBuilder {
        this.email = email
        return this
    }

    fun setPhotoUrl(photoUrl: Uri?): GoogleSignInAccountBuilder {
        this.photoUrl = photoUrl
        return this
    }

    fun build(): GoogleSignInAccount {
        val account = GoogleSignInAccount.createDefault()
        val accountClass = account.javaClass
        accountClass.getDeclaredField("zzbe").apply {
            isAccessible = true
            set(account, displayName)
        }
        accountClass.getDeclaredField("zzbq").apply {
            isAccessible = true
            set(account, email)
        }
        accountClass.getDeclaredField("zzbr").apply {
            isAccessible = true
            set(account, photoUrl)
        }
        return account
    }
}
