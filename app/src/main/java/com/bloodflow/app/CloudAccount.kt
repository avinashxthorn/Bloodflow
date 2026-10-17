package com.bloodflow.app

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private fun ready() = listOf(BuildConfig.FIREBASE_API_KEY,BuildConfig.FIREBASE_APP_ID,
    BuildConfig.FIREBASE_PROJECT_ID,BuildConfig.FIREBASE_WEB_CLIENT_ID).all { it.isNotBlank() }

private fun initialize(context: Context): Boolean {
    if (!ready()) return false
    if (FirebaseApp.getApps(context).isNotEmpty()) return true
    val options=FirebaseOptions.Builder().setApplicationId(BuildConfig.FIREBASE_APP_ID)
        .setApiKey(BuildConfig.FIREBASE_API_KEY).setProjectId(BuildConfig.FIREBASE_PROJECT_ID).build()
    FirebaseApp.initializeApp(context,options)
    return true
}
private fun derive(passphrase: String, salt: ByteArray): SecretKeySpec {
    val spec=PBEKeySpec(passphrase.toCharArray(),salt,210000,256)
    val bits=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    spec.clearPassword()
    return SecretKeySpec(bits,"AES")
}
private fun encrypt(data: String, password: String): String {
    val rnd=SecureRandom()
    val salt=ByteArray(16).also(rnd::nextBytes)
    val nonce=ByteArray(12).also(rnd::nextBytes)
    val cipher=Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE,derive(password,salt),GCMParameterSpec(128,nonce))
    val encrypted=cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    fun b(x:ByteArray)=Base64.encodeToString(x,Base64.NO_WRAP)
    return JSONObject().put("v",1).put("s",b(salt)).put("n",b(nonce)).put("c",b(encrypted)).toString()
}
private fun decrypt(data:String,password:String):String {
    val o=JSONObject(data)
    require(o.getInt("v")==1)
    fun b(key:String)=Base64.decode(o.getString(key),Base64.NO_WRAP)
    val cipher=Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE,derive(password,b("s")),GCMParameterSpec(128,b("n")))
    return String(cipher.doFinal(b("c")),Charsets.UTF_8)
}

@Composable
fun CloudAccountSection(onRestore:(String)->Boolean) {
    val ctx=androidx.compose.ui.platform.LocalContext.current
    val scope= rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf(if(ready() && initialize(ctx)) FirebaseAuth.getInstance().currentUser else null) }
    var message by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var askRestore by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    val snapshot=ctx.getSharedPreferences("bloodflow_local",Context.MODE_PRIVATE).getString("data",null)
    Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=androidx.compose.ui.graphics.Color.White),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Google account & backup",style=MaterialTheme.typography.titleLarge)
            Text("Optional sign-in. Your sensitive entries remain local until you explicitly back them up.",style=MaterialTheme.typography.bodySmall)
            if(!ready()) {
                Text("Google sign-in is not connected yet. Add Firebase project settings in GitHub Actions secrets. Guest tracking still works.",color=MaterialTheme.colorScheme.error)
            } else if(user==null) {
                Button(enabled=!busy,onClick={
                    message=""
                    busy=true
                    scope.launch {
                        try {
                            // Explicit sign-in button: Google's button API can prompt for a
                            // Google account even when the automatic account sheet has none.
                            val activity=ctx as? Activity ?: error("Google sign-in requires an active screen")
                            val option=GetSignInWithGoogleOption.Builder(BuildConfig.FIREBASE_WEB_CLIENT_ID).build()
                            val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
                            val result=CredentialManager.create(activity).getCredential(
                                context=activity, request=request
                            )
                            val cred=result.credential as? CustomCredential ?: error("Unexpected Google credential")
                            if(cred.type!=GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) error("Not a Google credential")
                            val token=GoogleIdTokenCredential.createFrom(cred.data).idToken
                            val auth=FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token,null)).await()
                            user=auth.user
                            message="Signed in. Your local history has not been uploaded."
                        } catch (_: GetCredentialCancellationException) {
                            message="Google sign-in was cancelled. You can try again."
                        } catch (_: NoCredentialException) {
                            message="No Google account was offered. Check that a Google account is added to your phone and that Google Play services is up to date, then try again."
                        } catch(e:Exception) {
                            message="Google sign-in could not complete: ${e.localizedMessage ?: "Try again or continue without an account."}"
                        }
                        busy=false
                    }
                }) { Text("Continue with Google") }
            } else {
                Text("Signed in: ${user?.email ?: user?.displayName ?: "Google user"}")
                OutlinedTextField(value=password,onValueChange={password=it.take(128)},label={Text("Backup encryption passphrase")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())
                Text("Use 12+ characters and remember this passphrase. We cannot recover it.",style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(value=confirmPass,onValueChange={confirmPass=it.take(128)},label={Text("Confirm passphrase for backup")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())
                Button(enabled=!busy && password.length>=12 && password==confirmPass && snapshot!=null,onClick={
                    busy=true
                    scope.launch {
                        try {
                            val cipher=withContext(Dispatchers.Default){encrypt(snapshot!!,password)}
                            FirebaseFirestore.getInstance().collection("bloodflow_backups").document(user!!.uid)
                                .set(mapOf("encrypted" to cipher,"format" to 1,"updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())).await()
                            message="Encrypted backup saved to your Google account."
                        } catch(e:Exception){message="Backup failed: ${e.localizedMessage}"}
                        busy=false
                    }
                }) {Text("Create / replace encrypted backup")}
                OutlinedButton(enabled=!busy && password.isNotBlank(),onClick={
                    busy=true
                    scope.launch {
                        try {
                            val snap=FirebaseFirestore.getInstance().collection("bloodflow_backups").document(user!!.uid)
                                .get(com.google.firebase.firestore.Source.SERVER).await()
                            val encrypted=snap.getString("encrypted") ?: error("No cloud backup found")
                            val raw=withContext(Dispatchers.Default){decrypt(encrypted,password)}
                            val decoded=JSONObject(raw)
                            require(decoded.optJSONArray("periods")!=null && decoded.optJSONArray("entries")!=null)
                            pendingRestore=raw
                            askRestore=true
                            message="Backup decrypted. Confirm before replacing this device's records."
                        } catch(e:Exception){message="Restore failed. Check internet and passphrase: ${e.localizedMessage}"}
                        busy=false
                    }
                }) {Text("Find & decrypt cloud backup")}
                OutlinedButton(enabled=!busy,onClick={ FirebaseAuth.getInstance().signOut();user=null;password="";confirmPass="";message="Signed out. Local data remains on this device." }) {Text("Sign out")}
            }
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
        }
    }
    if(askRestore && pendingRestore!=null) AlertDialog(
        onDismissRequest={askRestore=false;pendingRestore=null},
        title={Text("Replace local Bloodflow history?")},
        text={Text("This overwrites period dates, journal entries, name and cycle settings on this phone. This action cannot be undone. Make a separate backup of any important local data first.")},
        confirmButton={TextButton(onClick={
            val ok=onRestore(pendingRestore!!)
            message=if(ok) "Restore completed." else "Restore failed; local data not replaced."
            pendingRestore=null;askRestore=false
        }){Text("Replace with backup")}},
        dismissButton={TextButton(onClick={askRestore=false;pendingRestore=null}){Text("Cancel")}}
    )
}
