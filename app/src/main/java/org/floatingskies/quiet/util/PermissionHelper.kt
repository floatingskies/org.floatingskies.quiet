package org.floatingskies.quiet.util

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centraliza todas as verificações e pedidos de permissão.
 *
 * Ordem lógica (igual à tela de Onboarding):
 *   1. PHONE_STATE + READ_CALL_LOG (receber evento de chamada)
 *   2. READ_CONTACTS (importar contatos para a whitelist)
 *   3. ANSWER_PHONE_CALLS (Android 8+ - recusar chamada)
 *   4. POST_NOTIFICATIONS (Android 13+)
 *   5. Bateria/Otimização (Android 6+ - para o app não ser morto em segundo plano)
 */
object PermissionHelper {

    const val RC_PERMISSOES = 1001
    const val RC_BATERIA = 1003
    const val RC_ROLE_CALL_SCREENING = 1004

    /** Lista de permissões necessárias conforme a versão do Android. */
    fun permissoesNecessarias(): Array<String> {
        val lista = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lista.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lista.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            lista.add(Manifest.permission.CALL_PHONE)
        }
        return lista.toTypedArray()
    }

    fun todasConcedidas(context: Context): Boolean {
        return permissoesNecessarias().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun pedirTodas(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            permissoesNecessarias(),
            RC_PERMISSOES
        )
    }

    /** Android 6+: ignorar otimização de bateria (para o serviço não ser morto). */
    fun temIgnorarBateria(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    fun pedirIgnorarBateria(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !temIgnorarBateria(activity)) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, RC_BATERIA)
            } catch (_: Exception) {
                // Alguns fabricantes não suportam; abrir tela geral de bateria
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Android 10+: verifica se o app tem o ROLE_CALL_SCREENING.
     *
     * Isso permite bloquear chamadas SEM precisar ser o discador padrão.
     * O Google Telefone (ou Samsung Phone, etc.) continua sendo o discador
     * padrão do usuário — o Quiet só recebe o callback de CallScreeningService
     * para decidir se bloqueia ou não.
     *
     * Esta é a abordagem usada por apps como Truecaller e Should I Answer.
     */
    fun temRoleCallScreening(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            } catch (_: Exception) {
                false
            }
        } else {
            // Android 5-9: não precisa do role, o CallScreeningService funciona direto
            true
        }
    }

    /**
     * Android 10+: pede ao usuário o role de "app de filtragem de chamadas".
     *
     * Mostra um diálogo nativo do Android (não sai do app) perguntando:
     * "Permitir que o Quiet filtre chamadas?"
     *
     * Se o usuário aceitar, o Quiet passa a receber todos os callbacks de
     * CallScreeningService, podendo bloquear chamadas silenciosamente.
     * O discador padrão (Google Telefone) continua funcionando normalmente.
     */
    fun pedirRoleCallScreening(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    activity.startActivityForResult(intent, RC_ROLE_CALL_SCREENING)
                }
            } catch (_: Exception) {
                // Fallback: se RoleManager falhar, tenta definir como discador padrão
                abrirAppsPadrao(activity)
            }
        }
    }

    /** Abre a tela de apps padrão (fallback caso ROLE_CALL_SCREENING não funcione). */
    fun abrirAppsPadrao(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            }
        }
    }
}