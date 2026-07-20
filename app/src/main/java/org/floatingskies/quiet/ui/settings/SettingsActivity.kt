package org.floatingskies.quiet.ui.settings

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.floatingskies.quiet.App
import org.floatingskies.quiet.R
import org.floatingskies.quiet.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val prefs = App.instance.prefs

        // Protection toggle
        binding.switchProtecao.isChecked = prefs.protecaoAtiva
        binding.switchProtecao.setOnCheckedChangeListener { _, checked ->
            prefs.protecaoAtiva = checked
        }

        // Block hidden numbers
        binding.switchOcultos.isChecked = prefs.bloquearOcultos
        binding.switchOcultos.setOnCheckedChangeListener { _, checked ->
            prefs.bloquearOcultos = checked
        }

        // Block international numbers
        binding.switchInternacionais.isChecked = prefs.bloquearInternacionais
        binding.switchInternacionais.setOnCheckedChangeListener { _, checked ->
            prefs.bloquearInternacionais = checked
        }

        // Emergency bypass
        binding.switchEmergencia.isChecked = prefs.emergencyBypass
        binding.switchEmergencia.setOnCheckedChangeListener { _, checked ->
            prefs.emergencyBypass = checked
        }

        // Notify on block
        binding.switchNotificar.isChecked = prefs.notificarBloqueio
        binding.switchNotificar.setOnCheckedChangeListener { _, checked ->
            prefs.notificarBloqueio = checked
        }

        // Night schedule toggle
        binding.switchHorarioNoturno.isChecked = prefs.horarioNoturnoAtivo
        binding.switchHorarioNoturno.setOnCheckedChangeListener { _, checked ->
            prefs.horarioNoturnoAtivo = checked
            binding.cardHorarioDetalhe.visibility = if (checked) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Night schedule detail: show current times and click to edit
        binding.cardHorarioDetalhe.visibility = if (prefs.horarioNoturnoAtivo) android.view.View.VISIBLE else android.view.View.GONE
        binding.txtHorarioNoturno.text = "${String.format("%02d:00", prefs.horarioNoturnoInicio)} - ${String.format("%02d:00", prefs.horarioNoturnoFim)}"
        binding.cardHorarioDetalhe.setOnClickListener { mostrarDialogHorario() }

        // Block prefix
        binding.txtPrefixoStatus.text = if (prefs.bloquearPrefixos.isNullOrBlank()) {
            getString(R.string.cfg_prefixo_nenhum)
        } else {
            getString(R.string.cfg_prefixo_ativo, prefs.bloquearPrefixos)
        }
        binding.cardPrefixo.setOnClickListener { mostrarDialogPrefixo() }

        // Version info
        binding.txtVersao.text = getString(R.string.cfg_versao)
    }

    private fun mostrarDialogHorario() {
        val prefs = App.instance.prefs
        val view = layoutInflater.inflate(R.layout.dialog_horario, null)
        val edtInicio = view.findViewById<EditText>(R.id.edtInicio)
        val edtFim = view.findViewById<EditText>(R.id.edtFim)
        edtInicio.setText(String.format("%02d", prefs.horarioNoturnoInicio))
        edtFim.setText(String.format("%02d", prefs.horarioNoturnoFim))

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cfg_horario_titulo)
            .setView(view)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val inicio = edtInicio.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 22
                val fim = edtFim.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 7
                prefs.horarioNoturnoInicio = inicio
                prefs.horarioNoturnoFim = fim
                binding.txtHorarioNoturno.text = "${String.format("%02d:00", inicio)} - ${String.format("%02d:00", fim)}"
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun mostrarDialogPrefixo() {
        val prefs = App.instance.prefs
        val view = layoutInflater.inflate(R.layout.dialog_prefixo, null)
        val edtPrefixos = view.findViewById<EditText>(R.id.edtPrefixos)
        edtPrefixos.setText(prefs.bloquearPrefixos ?: "")

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cfg_prefixo_titulo)
            .setMessage(R.string.cfg_prefixo_mensagem)
            .setView(view)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val texto = edtPrefixos.text.toString().trim()
                prefs.bloquearPrefixos = if (texto.isBlank()) null else texto
                binding.txtPrefixoStatus.text = if (texto.isBlank()) {
                    getString(R.string.cfg_prefixo_nenhum)
                } else {
                    getString(R.string.cfg_prefixo_ativo, texto)
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}