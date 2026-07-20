package org.floatingskies.quiet.ui.whitelist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.floatingskies.quiet.App
import org.floatingskies.quiet.R
import org.floatingskies.quiet.data.WhitelistEntity
import org.floatingskies.quiet.databinding.ActivityWhitelistBinding
import org.floatingskies.quiet.databinding.DialogAddWhitelistBinding
import org.floatingskies.quiet.databinding.DialogDigitarNumeroBinding
import org.floatingskies.quiet.databinding.ItemWhitelistBinding
import org.floatingskies.quiet.util.PhoneUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WhitelistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhitelistBinding
    private val adapter = WhitelistAdapter { item -> confirmarRemocao(item) }
    private var listaCompleta: List<WhitelistEntity> = emptyList()

    private val pegarContato = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            resolverContato(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhitelistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fabAdd.setOnClickListener { mostrarDialogAdicionar() }

        // Search/filter
        binding.edtPesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        carregar()
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun filtrarLista(query: String) {
        val filtrada = if (query.isBlank()) {
            listaCompleta
        } else {
            listaCompleta.filter {
                it.nome.contains(query, ignoreCase = true) ||
                it.telefoneOriginal.contains(query, ignoreCase = true) ||
                it.telefone.contains(query)
            }
        }
        adapter.submitList(filtrada)
        if (filtrada.isEmpty() && listaCompleta.isNotEmpty()) {
            binding.txtContagem.text = getString(R.string.wl_vazio_filtro)
        } else {
            binding.txtContagem.text = getString(R.string.wl_contagem, listaCompleta.size)
        }
    }

    private fun carregar() {
        lifecycleScope.launch {
            listaCompleta = withContext(Dispatchers.IO) {
                App.instance.database.whitelistDao().listarTodos()
            }
            binding.txtContagem.text = getString(R.string.wl_contagem, listaCompleta.size)
            filtrarLista(binding.edtPesquisa.text?.toString()?.trim() ?: "")
        }
    }

    private fun mostrarDialogAdicionar() {
        val dialogBinding = DialogAddWhitelistBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnImportar.setOnClickListener {
            dialog.dismiss()
            abrirSeletorContato()
        }
        dialogBinding.btnDigitar.setOnClickListener {
            dialog.dismiss()
            mostrarDialogDigitar()
        }
        dialogBinding.btnImportarTodos.setOnClickListener {
            dialog.dismiss()
            confirmarImportarTodos()
        }
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmarImportarTodos() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.wl_importar_todos_titulo)
            .setMessage(R.string.wl_importar_todos_msg)
            .setPositiveButton(R.string.wl_sim) { _, _ ->
                importarTodosContatos()
            }
            .setNegativeButton(R.string.wl_cancelar, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun importarTodosContatos() {
        lifecycleScope.launch {
            try {
                val contatos = mutableListOf<Pair<String, String>>()
                val cursor = withContext(Dispatchers.IO) {
                    contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null, null,
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                    )
                }
                cursor?.use {
                    while (it.moveToNext()) {
                        val nome = it.getString(0) ?: continue
                        val telefone = it.getString(1) ?: continue
                        contatos.add(Pair(nome, telefone))
                    }
                }

                val db = App.instance.database
                val existentes = withContext(Dispatchers.IO) {
                    db.whitelistDao().listarTodos().map { it.telefone }.toSet()
                }
                var adicionados = 0
                for ((nome, telefone) in contatos) {
                    val normalizado = PhoneUtils.normalizar(telefone)
                    if (normalizado.length >= 10 && normalizado !in existentes) {
                        withContext(Dispatchers.IO) {
                            db.whitelistDao().inserir(WhitelistEntity(
                                nome = nome,
                                telefone = normalizado,
                                telefoneOriginal = PhoneUtils.formatar(telefone)
                            ))
                        }
                        adicionados++
                    }
                }

                MaterialAlertDialogBuilder(this@WhitelistActivity)
                    .setMessage(getString(R.string.wl_importar_todos_ok) + "\n$adicionados contatos adicionados.")
                    .setPositiveButton(R.string.ok, null)
                    .show()
                carregar()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@WhitelistActivity)
                    .setTitle(R.string.erro_desconhecido)
                    .setMessage(R.string.wl_erro_contato)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
    }

    private fun mostrarDialogDigitar() {
        val dialogBinding = DialogDigitarNumeroBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnSalvar.setOnClickListener {
            val nome = dialogBinding.edtNome.text.toString().trim()
            val telefone = dialogBinding.edtTelefone.text.toString().trim()
            if (nome.isBlank() || telefone.isBlank()) {
                dialogBinding.edtNome.error = if (nome.isBlank()) "Informe o nome" else null
                dialogBinding.edtTelefone.error = if (telefone.isBlank()) "Informe o telefone" else null
                return@setOnClickListener
            }
            salvar(nome, telefone)
            dialog.dismiss()
        }
        dialogBinding.btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun abrirSeletorContato() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        pegarContato.launch(intent)
    }

    private fun resolverContato(uri: android.net.Uri) {
        try {
            contentResolver.query(
                uri, arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nome = cursor.getString(0)
                    val telefone = cursor.getString(1)
                    salvar(nome, telefone)
                }
            }
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.erro_desconhecido)
                .setMessage(R.string.wl_erro_contato)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    private fun salvar(nome: String, telefone: String) {
        lifecycleScope.launch {
            val db = App.instance.database
            val normalizado = PhoneUtils.normalizar(telefone)

            if (normalizado.length < 10) {
                MaterialAlertDialogBuilder(this@WhitelistActivity)
                    .setMessage("Número muito curto. Verifique se incluiu o DDD.")
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return@launch
            }

            val existente = withContext(Dispatchers.IO) {
                db.whitelistDao().buscarPorTelefone(normalizado)
            }
            if (existente != null) {
                MaterialAlertDialogBuilder(this@WhitelistActivity)
                    .setMessage(getString(R.string.wl_erro_duplicado))
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return@launch
            }

            withContext(Dispatchers.IO) {
                db.whitelistDao().inserir(WhitelistEntity(
                    nome = nome,
                    telefone = normalizado,
                    telefoneOriginal = PhoneUtils.formatar(telefone)
                ))
            }
            carregar()
        }
    }

    private fun confirmarRemocao(item: WhitelistEntity) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.wl_remover_pergunta, item.nome))
            .setPositiveButton(R.string.wl_sim) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        App.instance.database.whitelistDao().deletar(item)
                    }
                    carregar()
                }
            }
            .setNegativeButton(R.string.wl_cancelar, null)
            .show()
    }
}

class WhitelistAdapter(
    private val onRemover: (WhitelistEntity) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<WhitelistAdapter.VH>() {

    private val items = mutableListOf<WhitelistEntity>()

    fun submitList(novaLista: List<WhitelistEntity>) {
        items.clear()
        items.addAll(novaLista)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemWhitelistBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWhitelistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.txtNome.text = item.nome
        holder.binding.txtTelefone.text = item.telefoneOriginal
        holder.binding.btnRemover.setOnClickListener { onRemover(item) }
    }

    override fun getItemCount() = items.size
}