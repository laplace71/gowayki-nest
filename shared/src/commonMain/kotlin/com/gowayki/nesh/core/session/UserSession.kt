package com.gowayki.nesh.core.session

import com.gowayki.nesh.domain.model.User

object UserSession {
    var currentUser: User? = null
        private set

    var activeDni: String = "71234567"

    val currentUserId: String?
        get() = currentUser?.id

    fun saveUser(user: User) {
        currentUser = user
        if (user.dni.isNotBlank()) {
            activeDni = user.dni
        }
    }

    fun clear() {
        currentUser = null
    }
}

