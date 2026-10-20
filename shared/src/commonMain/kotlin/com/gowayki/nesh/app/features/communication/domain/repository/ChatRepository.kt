// Contrato de la feature de Comunicación.
// Las pantallas y el ViewModel dependen solo de esta interfaz, nunca de la
// implementación. Hoy es FakeChatRepository (datos en memoria); cuando haya
// backend se cambia el cuerpo y la UI no se toca.
package com.gowayki.nesh.app.features.communication.domain.repository

import com.gowayki.nesh.app.features.communication.domain.model.Conversation
import com.gowayki.nesh.app.features.communication.domain.model.GroupMember
import com.gowayki.nesh.app.features.communication.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /** Lista de conversaciones del usuario, de la más reciente a la más antigua. */
    fun conversations(): Flow<List<Conversation>>

    /** Mensajes de una conversación, de más antiguo a más reciente. */
    fun messages(conversationId: String): Flow<List<Message>>

    /**
     * Envía un mensaje. El mensaje aparece de inmediato con estado SENDING y
     * el repositorio lo marca como DELIVERED a los pocos milisegundos.
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        kind: com.gowayki.nesh.app.features.communication.domain.model.MessageKind =
            com.gowayki.nesh.app.features.communication.domain.model.MessageKind.TEXT,
        audioDurationSeconds: Int = 0
    ): Result<Message>

    /** Marca el último mensaje ajeno como leído (actualiza el contador). */
    suspend fun markAsRead(conversationId: String): Result<Unit>

    /** Crea un grupo con el nombre dado y los miembros seleccionados. */
    suspend fun createGroup(name: String, members: List<GroupMember>): Result<Conversation>

    /** Crea (o devuelve) la conversación privada con ese conductor. */
    suspend fun openPrivateChat(member: GroupMember): Result<Conversation>

    /** Marca un mensaje como leído (doble check azul). */
    suspend fun markMessageRead(messageId: String): Result<Unit>

    /** Vacía los mensajes de una conversación, sin eliminarla. */
    suspend fun clearMessages(conversationId: String): Result<Unit>

    /** Elimina una conversación completa de la lista. */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /** Elimina un grupo (y sus mensajes). */
    suspend fun deleteGroup(conversationId: String): Result<Unit>
}