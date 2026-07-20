package org.floatingskies.quiet.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import org.floatingskies.quiet.App
import org.floatingskies.quiet.MainActivity
import org.floatingskies.quiet.R
import org.floatingskies.quiet.util.PhoneUtils
import org.floatingskies.quiet.util.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * CallScreeningService — Android 7.0 (API 24) ou superior.
 *
 * Esta é a forma OFICIAL e silenciosa de bloquear chamadas no Android moderno:
 *  - A chamada nunca toca, nunca aparece na tela, não gera "chamada perdida".
 *  - O usuário nem sabe que foi ligado.
 *
 * Comportamento:
 *   - Se proteção desativada → libera tudo
 *   - Se número de emergência e bypass ativo → libera
 *   - Se número oculto e "bloquear ocultos" ligado → bloqueia
 *   - Se whitelist vazia → bloqueia tudo (modo paranóia)
 *   - Se número está na whitelist → libera
 *   - Se internacional e "bloquear internacionais" ligado → bloqueia
 *   - Se no horário noturno → bloqueia (não-whitelisted)
 *   - Se DDD na lista de prefixos bloqueados → bloqueia
 *   - Senão → BLOQUEIA silenciosamente + registra no log
 */
@RequiresApi(Build.VERSION_CODES.N)
class CallBlockerService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: PrefsManager by lazy { App.instance.prefs }

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val telefoneRecebido = handle?.schemeSpecificPart ?: ""

        // Numbers PRIVATE/UNKNOWN chegam com handle vazio
        val ehOculto = PhoneUtils.ehOculto(telefoneRecebido)

        scope.launch {
            val deveBloquear = decidirBloqueio(telefoneRecebido, ehOculto)

            if (deveBloquear) {
                Log.i(TAG, "Bloqueando chamada: $telefoneRecebido (oculto=$ehOculto)")
                registrarBloqueio(telefoneRecebido)
                responderBloqueio(callDetails)

                // Show silent notification if enabled
                if (prefs.notificarBloqueio) {
                    val telefoneExibido = if (ehOculto) "Número oculto" else PhoneUtils.formatar(telefoneRecebido)
                    mostrarNotificacaoBloqueio(telefoneExibido)
                }
            } else {
                Log.i(TAG, "Permitindo chamada: $telefoneRecebido")
                responderPermitir(callDetails)
            }
        }
    }

    private suspend fun decidirBloqueio(telefone: String, ehOculto: Boolean): Boolean {
        if (!prefs.protecaoAtiva) return false

        // 1. Número oculto/privado
        if (ehOculto && prefs.bloquearOcultos) return true

        // 2. Emergency bypass (190, 192, 193, 197, 112, 911)
        if (prefs.emergencyBypass && PhoneUtils.isEmergency(telefone)) return false

        // 3. Whitelist vazia → bloqueia tudo (exceto emergência já tratada acima)
        val whitelist = App.instance.database.whitelistDao().listarTodos()
        if (whitelist.isEmpty()) return true

        // 4. Está na whitelist?
        val permitido = whitelist.any { PhoneUtils.corresponde(telefone, it.telefone) }
        if (permitido) return false

        // 5. International number blocking
        if (prefs.bloquearInternacionais && PhoneUtils.isInternational(telefone)) return true

        // 6. Night schedule: block ALL non-whitelist during night hours
        if (prefs.horarioNoturnoAtivo) {
            val hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (estaNoHorario(hora, prefs.horarioNoturnoInicio, prefs.horarioNoturnoFim)) {
                return true
            }
        }

        // 7. Area code (prefix) blocking
        val prefixos = prefs.bloquearPrefixos
        if (!prefixos.isNullOrBlank()) {
            val listaPrefixos = prefixos.split(",").map { it.trim() }
            val prefixoRecebido = PhoneUtils.getPrefixo(telefone)
            if (prefixoRecebido in listaPrefixos) return true
        }

        // Default: block if not on whitelist
        return true
    }

    private fun estaNoHorario(horaAtual: Int, inicio: Int, fim: Int): Boolean {
        return if (inicio <= fim) {
            horaAtual in inicio until fim
        } else {
            // Crosses midnight, e.g., 22:00 - 07:00
            horaAtual >= inicio || horaAtual < fim
        }
    }

    private suspend fun registrarBloqueio(telefone: String) {
        try {
            val telefoneOriginal = PhoneUtils.formatar(telefone)
            App.instance.database.blockedCallDao().registrarBloqueio(
                telefone = PhoneUtils.normalizar(telefone),
                telefoneOriginal = if (PhoneUtils.ehOculto(telefone)) "Número oculto" else telefoneOriginal
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar bloqueio", e)
        }
    }

    private fun mostrarNotificacaoBloqueio(telefone: String) {
        try {
            val channelId = CHANNEL_BLOQUEIO
            createNotificationChannelIfNeeded(channelId)

            val intent = android.content.Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(getString(R.string.notif_bloqueio_titulo))
                .setContentText(getString(R.string.notif_bloqueio_texto, telefone))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(null)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIF_BLOQUEIO_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mostrar notificação", e)
        }
    }

    private fun createNotificationChannelIfNeeded(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    getString(R.string.notif_bloqueio_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notif_bloqueio_channel_desc)
                    setSound(null, null)
                    enableVibration(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun responderBloqueio(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)              // recusa a chamada
            .setRejectCall(true)                // desliga na cara
            .setSkipCallLog(true)               // NÃO aparece no registro de chamadas
            .setSkipNotification(true)          // NÃO aparece notificação
            .build()
        respondToCall(callDetails, response)
    }

    private fun responderPermitir(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    companion object {
        private const val TAG = "CallBlockerService"
        private const val CHANNEL_BLOQUEIO = "quiet_bloqueio"
        private const val NOTIF_BLOQUEIO_ID = 2001
    }
}