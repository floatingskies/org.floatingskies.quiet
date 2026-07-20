package org.floatingskies.quiet.ui.blocked

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.floatingskies.quiet.App
import org.floatingskies.quiet.R
import org.floatingskies.quiet.data.BlockedCallEntity
import org.floatingskies.quiet.databinding.ActivityBlockedBinding
import org.floatingskies.quiet.databinding.ItemBlockedBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockedCallsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedBinding
    private val adapter = BlockedAdapter(
        onDelete = { item -> confirmarExclusao(item) },
        onLongPress = { item -> confirmarExclusao(item) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getItemAt(position) ?: return
                confirmarExclusao(item) {
                    adapter.remover(position)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recycler)

        binding.btnLimpar.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setMessage(R.string.bl_confirmar_limpar)
                .setPositiveButton(R.string.bl_confirmar_limpar_sim) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            App.instance.database.blockedCallDao().limparTudo()
                        }
                        carregar()
                    }
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }

        binding.btnExportar.setOnClickListener { exportarCsv() }

        carregar()
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            val lista = withContext(Dispatchers.IO) {
                App.instance.database.blockedCallDao().listarTodos()
            }
            if (lista.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recycler.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recycler.visibility = View.VISIBLE
                adapter.submitList(lista)
            }
        }
    }

    private fun confirmarExclusao(item: BlockedCallEntity, onConfirm: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.bl_confirmar_excluir, item.telefoneOriginal))
            .setPositiveButton(R.string.excluir) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        App.instance.database.blockedCallDao().deletarPorId(item.id)
                    }
                    if (onConfirm != null) {
                        onConfirm()
                    } else {
                        carregar()
                    }
                }
            }
            .setNegativeButton(R.string.cancelar) { _, _ ->
                // If swiped, reload to restore the item visually
                if (onConfirm != null) carregar()
            }
            .show()
    }

    private fun exportarCsv() {
        lifecycleScope.launch {
            val lista = withContext(Dispatchers.IO) {
                App.instance.database.blockedCallDao().listarTodos()
            }
            if (lista.isEmpty()) {
                MaterialAlertDialogBuilder(this@BlockedCallsActivity)
                    .setMessage(R.string.bl_vazia_exportar)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return@launch
            }

            try {
                val csv = File(cacheDir, "chamadas_bloqueadas_${System.currentTimeMillis()}.csv")
                FileWriter(csv).use { writer ->
                    writer.append("Telefone,Data,Hora,Vezes Bloqueada\n")
                    val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                    val sdfHora = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR"))
                    lista.forEach {
                        val date = Date(it.dataHora)
                        writer.append("\"${it.telefoneOriginal}\",")
                        writer.append("\"${sdfData.format(date)}\",")
                        writer.append("\"${sdfHora.format(date)}\",")
                        writer.append("${it.vezes}\n")
                    }
                }

                val uri = FileProvider.getUriForFile(
                    this@BlockedCallsActivity,
                    "${packageName}.fileprovider",
                    csv
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Exportar CSV"))
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@BlockedCallsActivity)
                    .setTitle(R.string.erro_desconhecido)
                    .setMessage(e.message ?: "Erro ao gerar CSV")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
    }
}

class BlockedAdapter(
    private val onDelete: (BlockedCallEntity) -> Unit,
    private val onLongPress: (BlockedCallEntity) -> Unit
) : RecyclerView.Adapter<BlockedAdapter.VH>() {

    private val items = mutableListOf<BlockedCallEntity>()
    private val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    fun submitList(novaLista: List<BlockedCallEntity>) {
        items.clear()
        items.addAll(novaLista)
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): BlockedCallEntity? {
        return items.getOrNull(position)
    }

    fun remover(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class VH(val binding: ItemBlockedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBlockedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.txtTelefone.text = item.telefoneOriginal
        holder.binding.txtData.text = "Última tentativa: ${sdf.format(Date(item.dataHora))}"
        holder.binding.txtVezes.text = "Bloqueada ${item.vezes} vez(es)"

        // Show scammer badge for 3+ blocks
        if (item.vezes >= 3) {
            holder.binding.txtScammerBadge.visibility = View.VISIBLE
        } else {
            holder.binding.txtScammerBadge.visibility = View.GONE
        }

        // Long press to delete
        holder.itemView.setOnLongClickListener {
            onLongPress(item)
            true
        }
    }

    override fun getItemCount() = items.size
}