package org.floatingskies.quiet.util

import android.telephony.PhoneNumberUtils
import java.util.Locale

/**
 * Utilitários para normalização e comparação de números de telefone brasileiros.
 *
 * Regras:
 * - Remove tudo que não é dígito
 * - Adiciona DDI 55 se faltar
 * - Aceita formatos com DDD 11 dígitos (celulares) ou 10 dígitos (fixos)
 */
object PhoneUtils {

    /** Normaliza para string só de dígitos, com DDI 55. */
    fun normalizar(telefone: String?): String {
        if (telefone.isNullOrBlank()) return ""
        var s = PhoneNumberUtils.stripSeparators(telefone)
        s = s.filter { it.isDigit() }

        // Remove prefixos comuns
        s = when {
            s.startsWith("00") && s.length > 4 -> s.drop(4)  // 0055 (operadora)
            s.startsWith("55") && s.length in 12..13 -> s    // já tem DDI
            s.startsWith("0") && s.length >= 11 -> s.drop(1) // 0 + DDD + número
            s.length in 10..11 -> "55$s"                     // sem DDI, adiciona
            else -> s
        }
        return s
    }

    /** Versão exibida ao usuário (ex: (11) 99999-9999). */
    fun formatar(telefone: String?): String {
        val n = normalizar(telefone).removePrefix("55")
        return when (n.length) {
            11 -> "(${n.substring(0, 2)}) ${n.substring(2, 7)}-${n.substring(7)}"
            10 -> "(${n.substring(0, 2)}) ${n.substring(2, 6)}-${n.substring(6)}"
            else -> telefone ?: ""
        }
    }

    /**
     * Comparação flexível para evitar falsos negativos por causa de DDI/0 extra.
     * Compara pelos últimos 8-9 dígitos (sufixo).
     */
    fun corresponde(telefoneRecebido: String?, telefoneWhitelist: String?): Boolean {
        val a = normalizar(telefoneRecebido).takeLast(11)
        val b = normalizar(telefoneWhitelist).takeLast(11)
        if (a.isBlank() || b.isBlank()) return false
        return a == b
    }

    fun ehOculto(telefone: String?): Boolean {
        if (telefone.isNullOrBlank()) return true
        val s = telefone.filter { it.isDigit() }
        return s.isBlank() || telefone.contains("UNKNOWN") || telefone.contains("PRIVATE")
    }

    /** Verifica se o número é um número de emergência brasileiro ou internacional. */
    fun isEmergency(telefone: String?): Boolean {
        if (telefone.isNullOrBlank()) return false
        val s = telefone.filter { it.isDigit() }
        val emergencias = setOf("190", "192", "193", "197", "112", "911", "156", "190")
        // Check if the number matches any emergency number
        return emergencias.any { s.endsWith(it) || s == it }
    }

    /** Verifica se o número é internacional (não começa com 55/DDI Brasil). */
    fun isInternational(telefone: String?): Boolean {
        if (telefone.isNullOrBlank()) return false
        val s = telefone.filter { it.isDigit() }
        // Se tem DDI e não é 55, é internacional
        if (s.length > 11) {
            // Verifica se o número tem DDI que não é Brasil
            val possiveisDDI = s.take(3).toIntOrNull()
            if (possiveisDDI != null && possiveisDDI != 55) return true
            val ddi2 = s.take(2).toIntOrNull()
            if (ddi2 != null && ddi2 != 55 && s.length in 12..15) return true
        }
        return false
    }

    /** Extrai o DDD (prefixo de 2 dígitos) de um número normalizado. */
    fun getPrefixo(telefone: String?): String {
        if (telefone.isNullOrBlank()) return ""
        val s = normalizar(telefone).removePrefix("55")
        return if (s.length >= 2) s.substring(0, 2) else ""
    }
}