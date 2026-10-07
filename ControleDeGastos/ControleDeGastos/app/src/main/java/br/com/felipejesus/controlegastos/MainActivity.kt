package br.com.felipejesus.controlegastos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.felipejesus.controlegastos.data.Tema
import br.com.felipejesus.controlegastos.ui.navegacao.AppNav
import br.com.felipejesus.controlegastos.ui.theme.ControleGastosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencias = (application as ControleGastosApp).container.preferencias
        setContent {
            val tema by preferencias.tema.collectAsStateWithLifecycle()
            val escuro = when (tema) {
                Tema.CLARO -> false
                Tema.ESCURO -> true
                Tema.SISTEMA -> isSystemInDarkTheme()
            }
            // Ícones da barra de status acompanham o tema escolhido no app.
            DisposableEffect(escuro) {
                val estilo = if (escuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }
            ControleGastosTheme(escuro = escuro) {
                AppNav()
            }
        }
    }
}
