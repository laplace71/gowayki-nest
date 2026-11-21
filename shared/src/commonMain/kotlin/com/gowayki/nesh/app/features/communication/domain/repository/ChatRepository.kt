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

    /** Conversaciones archivadas. */
    fun archived(): Flow<List<Conversation>>

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
        audioDurationSeconds: Int = 0,
        audioFilePath: String? = null
    ): Result<Message>

    /** Marca el último mensaje ajeno como leído (actualiza el contador). */
    suspend fun markAsRead(conversationId: String): Result<Unit>

    /** Crea un grupo con el nombre dado y los miembros seleccionados. */
    suspend fun createGroup(name: String, members: List<GroupMember>): Result<Conversation>

    /** Crea (o devuelve) la conversación privada con ese conductor. */
    suspend fun openPrivateChat(member: GroupMember): Result<Conversation>

    /** Marca un mensaje como leído (doble check azul). */
    suspend fun markMessageRead(messageId: String): Result<Unit>

    /**
     * Limpia una conversación SOLO para este usuario: guarda la marca en el
     * backend (sobrevive a reinstalar), oculta los mensajes anteriores y quita
     * el chat de la lista. No borra nada de la BD, no afecta a los demás y vuelve
     * a aparecer si llega un mensaje nuevo.
     */
    suspend fun clearMessages(conversationId: String): Result<Unit>

    /**
     * Oculta una conversación SOLO para el usuario actual.
     *
     * No borra mensajes, audios ni la conversación, y no cambia la membresía:
     * los demás participantes no notan nada. Permanece oculta aunque lleguen
     * mensajes nuevos; solo vuelve con [restaurarConversacion].
     */
    suspend fun ocultarConversacion(conversationId: String): Result<Unit>

    /** Conversaciones que este usuario ocultó, para poder restaurarlas. */
    fun hiddenConversations(): Flow<List<Conversation>>

    /** Devuelve a la lista una conversación ocultada con [ocultarConversacion]. */
    suspend fun restaurarConversacion(conversationId: String): Result<Unit>

    /**
     * Pide la eliminación definitiva de un grupo. No borra al momento: el
     * backend espera 72 horas y luego purga mensajes, audios y archivos.
     * Solo el creador o un administrador pueden pedirla.
     */
    suspend fun solicitarEliminacionGrupo(conversationId: String): Result<Unit>

    /** Cancela la eliminación definitiva antes de que se cumplan las 72 horas. */
    suspend fun cancelarEliminacionGrupo(conversationId: String): Result<Unit>

    /** Grupos con purga pendiente, para poder cancelarla antes de las 72 h. */
    fun eliminacionesPendientes(): Flow<List<Conversation>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    /** Archiva una conversación. */
    suspend fun archiveConversation(conversationId: String): Result<Unit>

    /** Desarchiva una conversación. */
    suspend fun unarchiveConversation(conversationId: String): Result<Unit>

    /** Actualiza la foto de una conversación. */
    suspend fun updateConversationPhoto(conversationId: String, path: String?): Result<Unit>

    /**
     * Elimina un mensaje.
     *
     * - `deleteForEveryone = false`: lo oculta SOLO para el usuario actual; se
     *   guarda en el backend y los demás participantes lo siguen viendo.
     * - `deleteForEveryone = true`: lo oculta para todos, pero solo lo permite
     *   el backend si el mensaje fue enviado por el usuario actual. El servidor
     *   guarda el contenido 72 horas y luego lo purga sin avisar al usuario.
     */
    suspend fun deleteMessage(conversationId: String, messageId: String, deleteForEveryone: Boolean): Result<Unit>

    /** Sale de un grupo: el grupo sigue existiendo, pero ya no lo ve el usuario. */
    suspend fun leaveGroup(conversationId: String): Result<Unit>

    /** Lista los participantes reales de una conversación (con nombre y rol). */
    suspend fun getParticipants(conversationId: String): Result<List<GroupMember>>

    /**
     * Agrega conductores a un grupo que quedó con un solo miembro. El backend
     * reutiliza el grupo de la subruta en vez de crear uno nuevo, así el
     * historial y el nombre se conservan.
     */
    suspend fun addMembers(conversationId: String, members: List<GroupMember>): Result<Unit>

    /**
     * Cambia el rol de un participante (ADMIN o MIEMBRO). Solo lo permite el
     * backend si quien lo pide es creador o ADMIN; el nuevo ADMIN hereda el
     * permiso y puede seguir asignando roles a otros.
     */
    suspend fun asignarRol(conversationId: String, userId: String, rol: String): Result<Unit>

    /** Nombre de la subruta concreta activa, para etiquetar conductores en la UI. */
    fun activeSubrouteLabel(): String? = null
}