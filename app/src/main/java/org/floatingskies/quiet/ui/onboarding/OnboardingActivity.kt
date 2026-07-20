package org.floatingskies.quiet.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.floatingskies.quiet.App
import org.floatingskies.quiet.MainActivity
import org.floatingskies.quiet.R
import org.floatingskies.quiet.databinding.ActivityOnboardingBinding
import org.floatingskies.quiet.util.PermissionHelper

/**
 * Tela de boas-vindas.
 *
 * Fluxo:
 *   1. Usuário vê os benefícios
 *   2. Concede todas as permissões (uma a uma)
 *   3. Concede ignorar bateria (Android 6+)
 *   4. Concede role de call screening (Android 10+)
 *   5. Vai para a MainActivity
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If onboarding was already completed, go straight to dashboard
        if (App.instance.prefs.onboardingConcluido) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        montarListaPermissoes()

        binding.btnConceder.setOnClickListener {
            PermissionHelper.pedirTodas(this)
        }

        binding.btnProximo.setOnClickListener {
            if (PermissionHelper.todasConcedidas(this)) {
                if (tudoProntoParaIniciar()) {
                    concluirEIrParaDashboard()
                } else {
                    checarBateriaERole()
                }
            } else {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.onb_falta_permissoes)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }

        // Already granted all? Check if we can skip onboarding entirely
        if (PermissionHelper.todasConcedidas(this) && tudoProntoParaIniciar()) {
            concluirEIrParaDashboard()
            return
        }
        if (PermissionHelper.todasConcedidas(this)) {
            binding.btnProximo.isEnabled = true
            binding.btnConceder.text = getString(R.string.onb_todas_concedidas)
            checarBateriaERole()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.RC_PERMISSOES) {
            atualizarStatusPermissoes()
            if (PermissionHelper.todasConcedidas(this)) {
                binding.btnProximo.isEnabled = true
                binding.btnConceder.text = getString(R.string.onb_todas_concedidas)
                checarBateriaERole()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PermissionHelper.RC_BATERIA,
            PermissionHelper.RC_ROLE_CALL_SCREENING -> {
                atualizarStatusPermissoes()
                verificarAvancoAutomatico()
            }
        }
    }

    private fun montarListaPermissoes() {
        val lista = binding.listaPermissoes
        lista.removeAllViews()

        val itens = listOf(
            ItemPermissao(R.string.onb_perm_phone, "phone") {
                PermissionHelper.todasConcedidas(this)
            },
            ItemPermissao(R.string.onb_perm_contacts, "contacts") {
                checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            },
            ItemPermissao(R.string.onb_perm_notifications, "notif") {
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            },
            ItemPermissao(R.string.onb_perm_battery, "battery") {
                PermissionHelper.temIgnorarBateria(this)
            },
            ItemPermissao(R.string.onb_perm_padrao, "padrao") {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) true
                else PermissionHelper.temRoleCallScreening(this)
            }
        )

        for (item in itens) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_permissao, lista, false)
            val txtTitulo = view.findViewById<TextView>(R.id.txtTitulo)
            val txtStatus = view.findViewById<TextView>(R.id.txtStatus)
            txtTitulo.setText(item.tituloRes)
            txtStatus.text = if (item.verificador()) "OK ✓" else "Conceder"
            val cor = if (item.verificador())
                ContextCompat.getColor(this, R.color.verde)
            else
                ContextCompat.getColor(this, R.color.ciano)
            txtStatus.setTextColor(cor)
            view.setOnClickListener {
                when (item.id) {
                    "phone", "notif" -> {
                        PermissionHelper.pedirTodas(this)
                    }
                    "contacts" -> {
                        requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), 1002)
                    }
                    "battery" -> PermissionHelper.pedirIgnorarBateria(this)
                    "padrao" -> PermissionHelper.pedirRoleCallScreening(this)
                }
            }
            lista.addView(view)
        }
    }

    private fun atualizarStatusPermissoes() {
        montarListaPermissoes()
    }

    private fun tudoProntoParaIniciar(): Boolean {
        if (!PermissionHelper.temIgnorarBateria(this)) return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
            !PermissionHelper.temRoleCallScreening(this)) return false
        return true
    }

    private fun concluirEIrParaDashboard() {
        App.instance.prefs.onboardingConcluido = true
        App.instance.prefs.protecaoAtiva = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun checarBateriaERole() {
        if (!PermissionHelper.temIgnorarBateria(this)) {
            MaterialAlertDialogBuilder(this)
                .setMessage("Permita ignorar otimização de bateria para que a proteção não seja desligada pelo sistema.")
                .setPositiveButton("Permitir") { _, _ ->
                    PermissionHelper.pedirIgnorarBateria(this)
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                   !PermissionHelper.temRoleCallScreening(this)) {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.onb_aviso_padrao)
                .setPositiveButton("Permitir") { _, _ ->
                    PermissionHelper.pedirRoleCallScreening(this)
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        } else {
            // All done, enable the start button
            binding.btnProximo.isEnabled = true
            binding.btnProximo.text = getString(R.string.onb_btn_comecar)
        }
    }

    private fun verificarAvancoAutomatico() {
        if (tudoProntoParaIniciar()) {
            concluirEIrParaDashboard()
        }
    }

    private data class ItemPermissao(
        val tituloRes: Int,
        val id: String,
        val verificador: () -> Boolean
    )
}