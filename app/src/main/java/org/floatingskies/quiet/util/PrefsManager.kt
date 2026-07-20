package org.floatingskies.quiet.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia preferências persistentes do app.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bloqueador_prefs", Context.MODE_PRIVATE)

    // ===== Configurações =====
    var bloquearOcultos: Boolean
        get() = prefs.getBoolean(KEY_BLOQUEAR_OCULTOS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOQUEAR_OCULTOS, value).apply()

    var protecaoAtiva: Boolean
        get() = prefs.getBoolean(KEY_PROTECAO_ATIVA, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECAO_ATIVA, value).apply()

    var onboardingConcluido: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var primeiroUso: Boolean
        get() = prefs.getBoolean(KEY_PRIMEIRO_USO, true)
        set(value) = prefs.edit().putBoolean(KEY_PRIMEIRO_USO, value).apply()

    // ===== Anti-scam settings =====
    var bloquearInternacionais: Boolean
        get() = prefs.getBoolean(KEY_BLOQUEAR_INTERNACIONAIS, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOQUEAR_INTERNACIONAIS, value).apply()

    var horarioNoturnoAtivo: Boolean
        get() = prefs.getBoolean(KEY_HORARIO_NOTURNO, false)
        set(value) = prefs.edit().putBoolean(KEY_HORARIO_NOTURNO, value).apply()

    var horarioNoturnoInicio: Int  // hour 0-23, default 22
        get() = prefs.getInt(KEY_HR_INICIO, 22)
        set(value) = prefs.edit().putInt(KEY_HR_INICIO, value).apply()

    var horarioNoturnoFim: Int  // hour 0-23, default 7
        get() = prefs.getInt(KEY_HR_FIM, 7)
        set(value) = prefs.edit().putInt(KEY_HR_FIM, value).apply()

    var notificarBloqueio: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICAR_BLOQUEIO, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICAR_BLOQUEIO, value).apply()

    var emergencyBypass: Boolean
        get() = prefs.getBoolean(KEY_EMERGENCY_BYPASS, true)
        set(value) = prefs.edit().putBoolean(KEY_EMERGENCY_BYPASS, value).apply()

    var bloquearPrefixos: String?  // comma-separated DDD codes, e.g. "11,21,31"
        get() = prefs.getString(KEY_BLOQUEAR_PREFIXOS, null)
        set(value) = prefs.edit().putString(KEY_BLOQUEAR_PREFIXOS, value).apply()

    companion object {
        private const val KEY_BLOQUEAR_OCULTOS = "bloquear_ocultos"
        private const val KEY_PROTECAO_ATIVA = "protecao_ativa"
        private const val KEY_ONBOARDING = "onboarding_concluido"
        private const val KEY_PRIMEIRO_USO = "primeiro_uso"
        private const val KEY_BLOQUEAR_INTERNACIONAIS = "bloquear_internacionais"
        private const val KEY_HORARIO_NOTURNO = "horario_noturno_ativo"
        private const val KEY_HR_INICIO = "horario_noturno_inicio"
        private const val KEY_HR_FIM = "horario_noturno_fim"
        private const val KEY_NOTIFICAR_BLOQUEIO = "notificar_bloqueio"
        private const val KEY_EMERGENCY_BYPASS = "emergency_bypass"
        private const val KEY_BLOQUEAR_PREFIXOS = "bloquear_prefixos"
    }
}