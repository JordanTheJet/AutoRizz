package com.autorizz.dating.convo

import com.autorizz.dating.db.ConversationMessageDao
import com.autorizz.dating.db.ConversationMessageEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val dao: ConversationMessageDao
) {
    suspend fun log(matchId: Long, direction: String, content: String): Long =
        dao.insert(
            ConversationMessageEntity(
                matchId = matchId,
                direction = direction,
                content = content
            )
        )

    suspend fun getHistory(matchId: Long): List<ConversationMessageEntity> =
        dao.getByMatch(matchId)

    suspend fun getRecent(matchId: Long, limit: Int = 20): List<ConversationMessageEntity> =
        dao.getRecentByMatch(matchId, limit)

    suspend fun messageCount(matchId: Long): Int = dao.countByMatch(matchId)
}
