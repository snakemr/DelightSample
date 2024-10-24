package my.example.delight.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import my.example.delight.data.DelightModel
import my.example.delight.db.User

@Composable
fun User(api: DelightModel.Api<User>, user: User) {
    var edit by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.CenterStart) {
        androidx.compose.animation.AnimatedVisibility(!edit, Modifier, fadeIn(), fadeOut()) {
            Text(user.name, Modifier.padding(12.dp).clickable { edit = api.active }, maxLines = 1)
        }
        androidx.compose.animation.AnimatedVisibility(edit, Modifier, fadeIn(), fadeOut()) {
            var editor by remember {
                mutableStateOf(TextFieldValue(user.name, TextRange(0, user.name.length)))
            }
            var focused by remember { mutableStateOf(false) }
            val requester = remember { FocusRequester() }
            LaunchedEffect(Unit) { requester.requestFocus() }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(editor, { editor = it },
                    Modifier.padding(12.dp).focusRequester(requester).onFocusChanged {
                        if (focused && !it.isFocused) edit = false else focused = it.isFocused
                    }.weight(1f),
                    textStyle = LocalTextStyle.current,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions {
                        if (api.active) {
                            edit = false
                            api.update(user.copy(name = editor.text))
                        }
                    }
                )
                IconButton({
                    edit = false
                    api.update(user.copy(name = editor.text))
                },  enabled = api.active,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, null)
                }
                IconButton({ api.delete(user) },
                    enabled = api.active,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
    }
}