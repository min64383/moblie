package com.example.mobile_project

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseGameService(context: Context) {
    private var auth: FirebaseAuth? = null
    private var database: FirebaseFirestore? = null
    val configured: Boolean get() = auth != null && database != null
    var setupMessage: String = "Firebase 설정이 없어요. google-services.json을 app 폴더에 넣고 다시 빌드해 주세요."
        private set

    init {
        try {
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(context.applicationContext)
            if (app != null) {
                auth = FirebaseAuth.getInstance(app)
                database = FirebaseFirestore.getInstance(app)
                setupMessage = "Firebase 설정을 읽었어요. 이메일 계정으로 로그인할 수 있어요."
            }
        } catch (_: Exception) {
            auth = null
            database = null
            setupMessage = "Firebase 설정을 읽지 못했어요. google-services.json의 앱 등록 정보를 확인해 주세요."
        }
    }

    fun observeUser(onChange: (OnlineUser?) -> Unit): AutoCloseable {
        val engine = auth
        if (engine == null) {
            onChange(null)
            return AutoCloseable { }
        }
        val listener = FirebaseAuth.AuthStateListener { current ->
            onChange(current.currentUser?.let { OnlineUser(it.uid, it.email.orEmpty()) })
        }
        engine.addAuthStateListener(listener)
        return AutoCloseable { engine.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String, register: Boolean) {
        val engine = requireNotNull(auth) { setupMessage }
        if (register) engine.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
        else engine.signInWithEmailAndPassword(email.trim(), password).awaitResult()
    }

    fun signOut() { auth?.signOut() }

    private fun document(uid: String) = requireNotNull(database) { setupMessage }
        .collection("users").document(uid).collection("saves").document("main")

    private fun requireUser(uid: String) {
        check(auth?.currentUser?.uid == uid) { "로그인 계정이 변경됐어요. 다시 로그인해 주세요." }
    }

    suspend fun read(uid: String): CloudSaveRecord? {
        requireUser(uid)
        // 서버 확인 실패를 '저장이 없음'으로 취급하지 않는다. 캐시로 덮어쓰기 여부를 결정하지 않는다.
        val saved = document(uid).get(Source.SERVER).awaitResult()
        requireUser(uid)
        if (!saved.exists()) return null
        require(saved.getLong("schemaVersion") == GameBackupCodec.VERSION.toLong()) { "지원하지 않는 클라우드 저장 버전이에요." }
        val revision = requireNotNull(saved.getLong("revision"))
        val payload = requireNotNull(saved.getString("payload"))
        require(revision > 0 && payload.toByteArray(Charsets.UTF_8).size <= GameBackupCodec.MAX_BYTES)
        return CloudSaveRecord(revision, saved.getTimestamp("updatedAt")?.toDate()?.time ?: 0L, payload,
            SaveSummary(requireNotNull(saved.getLong("semester")).toInt(), requireNotNull(saved.getLong("week")).toInt(),
                requireNotNull(saved.getLong("day")).toInt()))
    }

    suspend fun upload(uid: String, expectedRevision: Long, payload: String, summary: SaveSummary) {
        requireUser(uid)
        require(payload.toByteArray(Charsets.UTF_8).size <= GameBackupCodec.MAX_BYTES)
        val db = requireNotNull(database)
        val target = document(uid)
        db.runTransaction { transaction ->
            requireUser(uid)
            val current = transaction.get(target)
            val actual = if (current.exists()) requireNotNull(current.getLong("revision")) else 0L
            CloudSyncRules.requireRevision(expectedRevision, actual)
            transaction.set(target, mapOf(
                "schemaVersion" to GameBackupCodec.VERSION,
                "revision" to expectedRevision + 1L,
                "updatedAt" to FieldValue.serverTimestamp(),
                "semester" to summary.semester,
                "week" to summary.week,
                "day" to summary.day,
                "payload" to payload
            ))
            Unit
        }.awaitResult()
        requireUser(uid)
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            when {
                task.isCanceled -> continuation.cancel()
                task.isSuccessful -> continuation.resume(task.result)
                else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase 요청에 실패했어요."))
            }
        }
    }
}
