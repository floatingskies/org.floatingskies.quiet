package org.floatingskies.quiet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.floatingskies.quiet.databinding.ActivityMainBinding
import org.floatingskies.quiet.ui.blocked.BlockedCallsActivity
import org.floatingskies.quiet.ui.settings.SettingsActivity
import org.floatingskies.quiet.ui.whitelist.WhitelistActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        atualizarStatus()
        configurarCliques()
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
    }

    private fun atualizarStatus() {
        val prefs = App.instance.prefs

        // Toggle de proteção
        binding.switchProtecao.setOnCheckedChangeListener(null)
        binding.switchProtecao.isChecked = prefs.protecaoAtiva
        binding.switchProtecao.setOnCheckedChangeListener { _, isChecked ->
            prefs.protecaoAtiva = isChecked
            atualizarCardStatus()
        }
        atualizarCardStatus()

        // Contadores
        lifecycleScope.launch {
            val db = App.instance.database
            val whitelist = withContext(Dispatchers.IO) { db.whitelistDao().listarTodos() }
            val bloqueadas = withContext(Dispatchers.IO) { db.blockedCallDao().contar() }
            val bloqueadasHoje = withContext(Dispatchers.IO) { db.blockedCallDao().contarHoje() }

            binding.txtWhitelistCount.text = "${whitelist.size}"
            binding.txtBlockedToday.text = "$bloqueadasHoje"

            binding.txtBlockedCount.text = if (bloqueadas == 0) {
                getString(R.string.dash_card_blocked_desc)
            } else {
                "$bloqueadas ${getString(R.string.dash_blocked_total)}"
            }
        }
    }

    private fun atualizarCardStatus() {
        val prefs = App.instance.prefs
        if (prefs.protecaoAtiva) {
            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.ativo_verde))
            binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.ativo_verde_borda)
            binding.txtStatus.text = getString(R.string.dash_status_ativo)
            binding.txtStatus.setTextColor(ContextCompat.getColor(this, R.color.verde))
            binding.txtStatusDesc.text = getString(R.string.dash_status_bloqueando)
        } else {
            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.inativo_vermelho))
            binding.cardStatus.strokeColor = ContextCompat.getColor(this, R.color.inativo_vermelho_borda)
            binding.txtStatus.text = getString(R.string.dash_status_inativo)
            binding.txtStatus.setTextColor(ContextCompat.getColor(this, R.color.vermelho))
            binding.txtStatusDesc.text = getString(R.string.dash_status_livre)
        }
    }

    private fun configurarCliques() {
        binding.cardWhitelist.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }
        binding.cardBlocked.setOnClickListener {
            startActivity(Intent(this, BlockedCallsActivity::class.java))
        }
        binding.cardConfig.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}