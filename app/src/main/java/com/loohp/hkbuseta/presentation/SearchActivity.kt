package com.loohp.hkbuseta.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.presentation.theme.HKBusETATheme
import com.loohp.hkbuseta.presentation.utils.JsonUtils
import android.util.Pair


class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchPage(this)
        }
    }
}

fun defaultText(): String {
    return if (Shared.language == "en") "Input Route" else "輸入路線"
}

@Composable
fun SearchPage(instance: SearchActivity) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            Shared.MainTime()
        }
        MainElement(instance)
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun MainElement(instance: SearchActivity) {
    val state = remember { mutableStateOf(Pair.create(defaultText(), Registry.getInstance(instance).getPossibleNextChar(""))) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(35.dp)
                .border(2.dp, MaterialTheme.colors.secondaryVariant, RoundedCornerShape(10))
                .background(MaterialTheme.colors.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = state.value.first
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Column {
                KeyboardButton(instance, '7', state)
                KeyboardButton(instance, '4', state)
                KeyboardButton(instance, '1', state)
                KeyboardButton(instance, '<', state, Icons.Outlined.Delete, Color.Red)
            }
            Column {
                KeyboardButton(instance, '8', state)
                KeyboardButton(instance, '5', state)
                KeyboardButton(instance, '2', state)
                KeyboardButton(instance, '0', state)
            }
            Column {
                KeyboardButton(instance, '9', state)
                KeyboardButton(instance, '6', state)
                KeyboardButton(instance, '3', state)
                KeyboardButton(instance, '/', state, Icons.Outlined.Done, Color.Green)
            }
            Spacer(modifier = Modifier.size(10.dp))
            Box (
                modifier = Modifier
                    .width(35.dp)
                    .height(135.dp)
            ) {
                Column (
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    var alphabet = 'A'
                    while (alphabet <= 'Z') {
                        if (state.value.second.first.contains(alphabet)) {
                            KeyboardButton(instance, alphabet, state)
                        }
                        alphabet++
                    }
                }
            }
        }
    }
}

fun handleInput(instance: SearchActivity, state: MutableState<Pair<String, Pair<Set<Char>, Boolean>>>, input: Char) {
    var originalText = state.value.first
    if (originalText == defaultText()) {
        originalText = "";
    }
    if (input == '/') {
        val result = Registry.getInstance(instance).findRoutes(originalText)
        if (result != null && result.isNotEmpty()) {
            val intent = Intent(instance, ListRouteActivity::class.java)
            intent.putExtra("result", JsonUtils.fromCollection(result).toString())
            instance.startActivity(intent)
        }
    } else {
        val newText = if (input == '<') {
            if (originalText.isNotEmpty()) {
                originalText.dropLast(1)
            } else {
                originalText
            }
        } else {
            originalText + input
        }
        val possibleNextChar = Registry.getInstance(instance).getPossibleNextChar(newText)
        val text = newText.ifEmpty { defaultText() }
        state.value = Pair.create(text, possibleNextChar)
    }
}

@Composable
fun KeyboardButton(instance: SearchActivity, content: Char, state: MutableState<Pair<String, Pair<Set<Char>, Boolean>>>) {
    KeyboardButton(instance, content, state, null, MaterialTheme.colors.primary)
}


@Composable
fun KeyboardButton(instance: SearchActivity, content: Char, state: MutableState<Pair<String, Pair<Set<Char>, Boolean>>>, icon: ImageVector?, color: Color) {
    val enabled = when (content) {
        '/' -> state.value.second.second
        '<' -> true
        else -> state.value.second.first.contains(content)
    }
    val actualColor = if (enabled) color else Color(0xFF444444)
    Button(
        onClick = {
            handleInput(instance, state, content)
        },
        modifier = Modifier
            .width(35.dp)
            .height(if (content.isLetter()) 30.dp else 35.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = actualColor
        ),
        enabled = enabled,
        content = {
            if (icon == null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = actualColor,
                    text = content.toString()
                )
            } else {
                Icon(
                    modifier = Modifier.size(17.dp),
                    imageVector = icon,
                    contentDescription = content.toString(),
                    tint = actualColor,
                )
            }
        }
    )
}