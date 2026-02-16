package com.supereva.fluentai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.supereva.fluentai.ui.theme.FluentAiTheme
import com.supereva.fluentai.ui.navigation.MainNavHost
import com.supereva.fluentai.di.SessionServiceLocator


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionServiceLocator.init(this)
        enableEdgeToEdge()
        setContent {
            FluentAiTheme {
                MainNavHost()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FluentAiTheme {
        Greeting("Android")
    }
}