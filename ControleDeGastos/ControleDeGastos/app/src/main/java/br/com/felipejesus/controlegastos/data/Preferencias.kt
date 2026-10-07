package br.com.felipejesus.controlegastos.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tema { SISTEMA, CLARO, ESCURO }

class Preferencias(context: Context) {
    private val sp = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _tema = MutableStateFlow(
        runCatching { Tema.valueOf(sp.getString(K_TEMA, null) ?: Tema.SISTEMA.name) }
            .getOrDefault(Tema.SISTEMA)
    )
    val tema: StateFlow<Tema> = _tema.asStateFlow()

    private val _capturaNotificacoes = MutableStateFlow(sp.getBoolean(K_CAPTURA, false))
    val capturaNotificacoes: StateFlow<Boolean> = _capturaNotificacoes.asStateFlow()

    fun definirTema(tema: Tema) {
        sp.edit { putString(K_TEMA, tema.name) }
        _tema.value = tema
    }

    fun definirCapturaNotificacoes(ativa: Boolean) {
        sp.edit { putBoolean(K_CAPTURA, ativa) }
        _capturaNotificacoes.value = ativa
    }

    private companion object {
        const val K_TEMA = "tema"
        const val K_CAPTURA = "captura_notificacoes"
    }
}
