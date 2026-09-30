package com.pranvir.boardtocontact

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pranvir.boardtocontact.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resumeWithException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var photoUri: Uri? = null
    private var phones: List<String> = emptyList()
    private var rawText: String = ""

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri?.let { readBoard(it) }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { readBoard(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.captureButton.setOnClickListener { capture() }
        binding.galleryButton.setOnClickListener { pickImage.launch("image/*") }
        binding.saveButton.setOnClickListener { save() }
        binding.hideIconSwitch.isChecked = isIconHidden()
        binding.hideIconSwitch.setOnCheckedChangeListener { _, hide -> setIconHidden(hide) }

        checkForUpdate()
    }

    private fun capture() {
        val file = File(File(cacheDir, "captures").apply { mkdirs() }, "board.jpg")
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        photoUri = uri
        takePicture.launch(uri)
    }

    private fun readBoard(uri: Uri) {
        binding.progress.visibility = View.VISIBLE
        binding.preview.setImageURI(uri)
        lifecycleScope.launch {
            try {
                val bitmap = loadDownscaled(uri) ?: throw IllegalStateException("photo")
                val image = InputImage.fromBitmap(bitmap, 0)
                val text = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .await()
                onText(text.text, text.textBlocks.flatMap { it.lines }.map { it.text })
            } catch (_: Exception) {
                binding.progress.visibility = View.GONE
                toast(getString(R.string.read_failed))
            }
        }
    }

    /** suspend bridge for the Play-services Task API. */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it, null) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    private fun onText(full: String, lines: List<String>) {
        binding.progress.visibility = View.GONE
        rawText = full
        val parsed = BoardParse.parse(full, lines)
        binding.nameField.setText(parsed.name)
        binding.addressField.setText(parsed.address)
        phones = parsed.phones
        binding.phoneSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            if (phones.isEmpty()) listOf(getString(R.string.no_phone)) else phones,
        )
        binding.resultCard.visibility = View.VISIBLE
        if (parsed.name.isEmpty() && phones.isEmpty()) toast(getString(R.string.nothing_found))
    }

    private fun loadDownscaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 1920) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun save() {
        val name = binding.nameField.text.toString().trim()
        val address = binding.addressField.text.toString().trim()
        if (name.isEmpty() && phones.isEmpty()) {
            toast(getString(R.string.nothing_to_save))
            return
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
            phones.getOrNull(0)?.let {
                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, it)
            }
            phones.getOrNull(1)?.let {
                putExtra(android.provider.ContactsContract.Intents.Insert.SECONDARY_PHONE, it)
            }
            if (address.isNotEmpty()) {
                putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, address)
            }
            putExtra(android.provider.ContactsContract.Intents.Insert.NOTES, rawText.take(500))
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            toast(getString(R.string.no_contacts_app))
        }
    }

    private fun isIconHidden(): Boolean =
        packageManager.getComponentEnabledSetting(launcherAlias()) ==
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    private fun launcherAlias() =
        android.content.ComponentName(this, "com.pranvir.boardtocontact.Launcher")

    private fun setIconHidden(hide: Boolean) {
        packageManager.setComponentEnabledSetting(
            launcherAlias(),
            if (hide) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            android.content.pm.PackageManager.DONT_KILL_APP,
        )
        toast(getString(if (hide) R.string.icon_hidden else R.string.icon_shown))
    }

    private fun prefs() = getSharedPreferences("btc", MODE_PRIVATE)

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val latest = UpdateCheck.fetchLatestTag() ?: return@launch
            if (!UpdateCheck.isNewer(latest, BuildConfig.VERSION_NAME)) return@launch
            if (prefs().getString("skipped_version", null) == latest) return@launch
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(getString(R.string.update_title, latest))
                .setMessage(getString(R.string.update_message))
                .setPositiveButton(R.string.update_now) { _, _ ->
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse(UpdateCheck.RELEASES_PAGE),
                            ),
                        )
                    } catch (_: Exception) {
                        toast(getString(R.string.update_open_failed))
                    }
                }
                .setNeutralButton(R.string.update_skip) { _, _ ->
                    prefs().edit().putString("skipped_version", latest).apply()
                }
                .setNegativeButton(R.string.update_later, null)
                .show()
        }
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
