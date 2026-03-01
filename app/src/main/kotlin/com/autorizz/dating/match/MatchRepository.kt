package com.autorizz.dating.match

import com.autorizz.dating.db.MatchDao
import com.autorizz.dating.db.MatchEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val dao: MatchDao
) {
    suspend fun record(
        name: String,
        app: String,
        age: Int? = null,
        bioSummary: String? = null,
        profileScreenshot: String? = null
    ): Long = dao.insert(
        MatchEntity(
            name = name,
            app = app,
            age = age,
            bioSummary = bioSummary,
            profileScreenshot = profileScreenshot
        )
    )

    suspend fun getById(id: Long): MatchEntity? = dao.getById(id)

    suspend fun getAll(): List<MatchEntity> = dao.getAll()

    suspend fun getRecent(limit: Int = 50): List<MatchEntity> = dao.getRecent(limit)

    suspend fun getByStatus(status: String): List<MatchEntity> = dao.getByStatus(status)

    suspend fun getByApp(app: String): List<MatchEntity> = dao.getByApp(app)

    suspend fun updateStatus(id: Long, status: String) = dao.updateStatus(id, status)

    suspend fun updateLastMessage(id: Long) =
        dao.updateLastMessage(id, System.currentTimeMillis())

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun countByStatus(status: String): Int = dao.countByStatus(status)

    suspend fun countByApp(app: String): Int = dao.countByApp(app)
}
