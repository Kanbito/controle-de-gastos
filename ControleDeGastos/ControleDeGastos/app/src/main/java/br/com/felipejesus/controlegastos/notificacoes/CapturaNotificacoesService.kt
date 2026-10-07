package br.com.felipejesus.controlegastos.notificacoes

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import br.com.felipejesus.controlegastos.ControleGastosApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lê notificações de outros apps (bancos, carteiras digitais) e, quando encontra
 * um valor em reais, guarda como "pendente" para o usuário confirmar.
 * Nada é lançado automaticamente e nada sai do aparelho.
 */
class CapturaNotificacoesService : NotificationListenerService() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val app = application as? ControleGastosApp ?: return
        val container = app.container
        if (!container.preferencias.capturaNotificacoes.value) return
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        val notificacao = sbn.notification ?: return
        if (notificacao.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notificacao.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val corpo = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val texto = listOf(titulo, corpo).filter { it.isNotBlank() }.joinToString(" — ")

        val resultado = ParserNotificacao.analisar(texto) ?: return
        val nomeApp = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)

        escopo.launch {
            container.repositorio.registrarNotificacao(
                valorCentavos = resultado.valorCentavos,
                tipo = resultado.tipo,
                texto = texto,
                app = nomeApp,
                recebidaEm = sbn.postTime,
            )
        }
    }

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }

    companion object {
        fun acessoConcedido(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Abre direto a tela de permissão deste app (Android 11+). */
        @RequiresApi(30)
        fun intentConfiguracoes(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, CapturaNotificacoesService::class.java).flattenToString()
                )
            }

        fun intentConfiguracoesGeral(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
}
