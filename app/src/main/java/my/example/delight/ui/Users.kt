package my.example.delight.ui

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import my.example.delight.data.DelightModel
import my.example.delight.db.User

@Composable
fun Users(api: DelightModel.Api<User>) {
    val users by api.all().collectAsState(emptyList())

    LazyColumn {
        items(users.sortedBy { it.id }, { it.id }) {
            Column(Modifier.animateItem()) {
                User(api, it)
                HorizontalDivider()
            }
        }
        item(0) {
            TextButton({
                api.insert(User(0, "Новый"))
            }, Modifier.animateItem(), api.active) {
                Text("Добавить")
            }
        }
    }
}