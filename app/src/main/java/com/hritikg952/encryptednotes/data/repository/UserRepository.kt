package com.hritikg952.encryptednotes.data.repository

import com.hritikg952.encryptednotes.data.db.UserDao
import com.hritikg952.encryptednotes.data.model.User

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun getUser(): User? = userDao.getUser()
}
