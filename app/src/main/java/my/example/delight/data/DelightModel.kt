package my.example.delight.data

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import my.example.delight.data.DelightModel.Api
import my.example.delight.db.Database
import my.example.delight.db.Database.Companion.invoke
import my.example.delight.db.User
import kotlin.coroutines.coroutineContext

class DelightModel(app: Application): AndroidViewModel(app) {
    private val driver = AndroidSqliteDriver(Database.Schema, app, "database.db")
    private val database = Database(driver)

    val dbUsers by lazy {
        object : Api<User> {
            private val queries = database.userQueries
            override fun all() = queries.all().flow()
            override fun insert(user: User) = request { queries.insert(user.name) }
            override fun update(user: User) = request { queries.update(user.name, user.id) }
            override fun delete(user: User) = request { queries.delete(user.id) }
            override val active = true
        }
    }

    private val api = ApiClient("10.0.2.2", secured = false)
    private var active by mutableStateOf(false)

    val apiUsers by lazy {
        object : Api<User> {
            override fun all() = api.realtime().flow({ api.users() }) { first.id == second.id }
            override fun insert(user: User) = request { api.insert(user.name) }
            override fun update(user: User) = request { api.update(user) }
            override fun delete(user: User) = request { api.delete(user.id) }
            override val active get() = this@DelightModel.active
        }
    }

    val nopUsers by lazy {
        object : Api<User> {
            override fun all() = emptyFlow<List<User>>()
            override fun insert(user: User) = Job()
            override fun update(user: User) = Job()
            override fun delete(user: User) = Job()
            override val active get() = false
        }
    }

    interface Api<T> {
        fun all(): Flow<List<T>>
        fun insert(entry: T): Job
        fun update(entry: T): Job
        fun delete(entry: T): Job
        val active: Boolean
    }

    var error: String? by mutableStateOf(null)

    private fun <T: Any> Query<T>.flow() =
        asFlow().mapToList(Dispatchers.IO)

    private fun <T> Flow<ApiClient.DataAction<T>>.flow(
        initial: suspend ()->List<T>, equals: Pair<T,T>.()-> Boolean
    ) = flow {
        active = false
        emit(emptyList())
        while (coroutineContext.isActive) {
            val list = try {
                initial().also {
                    emit(it)
                    active = true
                }.toMutableStateList()
            } catch (e: Exception) {
                if (e !is CancellationException) error = e.message
                continue
            } finally { }

            catch {
                error = it.message
            }.collect { (action, data) ->
                when (action) {
                    ApiClient.Action.Insert -> list += data
                    ApiClient.Action.Delete -> list.removeAll { equals(it to data) }
                    ApiClient.Action.Update -> {
                        val i = list.indexOfFirst { equals(it to data) }
                        if (i >= 0) list[i] = data
                    }
                }
                emit(list)
            }
            active = false
            if (error == null) error = "Socket disconnected"
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private inline fun request(crossinline block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                block()
            }
        }
}