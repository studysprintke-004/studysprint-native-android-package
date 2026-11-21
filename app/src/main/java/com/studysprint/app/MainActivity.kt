package com.studysprint.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

data class StudyDocument(
    val id: String,
    val name: String,
    val category: String,
    val folderPath: String,
    val university: String
)

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var universitySpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var searchInput: EditText
    private lateinit var statusText: TextView
    private lateinit var documentList: LinearLayout
    private lateinit var showOfflineButton: Button

    private var allDocuments: List<StudyDocument> = emptyList()
    private var offlineMode = false

    private val sources = linkedMapOf(
        "Egerton University" to "https://script.google.com/macros/s/AKfycbzxAK51VBJ3QM2gSMD9SO0MNkDHRHezu8sSRrD9et3usv0zEIwNhhIPt257cYjwohQ9/exec",
        "Kenyatta University" to "https://script.google.com/macros/s/AKfycbzbxbzgXhmorxQlspbQ_jr5t5fI3TxSi2wFZgQvjjXoS4vGCBB_gq-6EIYIXhbGrEJamg/exec",
        "Chuka University" to "https://script.google.com/macros/s/AKfycbxmq-cKyV78X6UFXM01XPEMkwBqlsz5zJ6O95jS7Y7LV-Yc5Jqqe7nYAGWc-cu1ryHa/exec",
        "Murang'a University" to "https://script.google.com/macros/s/AKfycbxdgvqK4SoinPBmpwohHy-qO8y2QQg7vT1cZQxthuFG-dKXB6jeVSoNsvIQBIwXtQHWEg/exec",
        "Meru University of Science and Technology" to "https://script.google.com/macros/s/AKfycbwf0fv537Ks7lBNYYDgTGWp71J_qXB4GNA7MiYs2dMSofcK7VOQXx-76vTa7gbnYDrx/exec",
        "Kisii University" to "https://script.google.com/macros/s/AKfycbz1xeOSt3bTczJbNkJDUKXleWHYd0ZJWZ7itPTQpwwePvqLeflnbsrkeZq6Sj9qVPup/exec"
    )

    private val categories = listOf(
        "All categories", "CATs", "Notes", "Past Papers", "Reference Books"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        loadUniversityDocuments()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F7F5.toInt())
        }

        val header = TextView(this).apply {
            text = "StudySprint"
            textSize = 25f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(20, 22, 20, 6)
            setBackgroundColor(0xFF064E3B.toInt())
        }
        page.addView(header)

        val subtitle = TextView(this).apply {
            text = "Your university revision library"
            textSize = 14f
            setTextColor(0xFFE0F2E9.toInt())
            setPadding(20, 0, 20, 18)
            setBackgroundColor(0xFF064E3B.toInt())
        }
        page.addView(subtitle)

        universitySpinner = Spinner(this)
        universitySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, sources.keys.toList()
        )
        page.addView(label("Choose university"))
        page.addView(universitySpinner)

        categorySpinner = Spinner(this)
        categorySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, categories
        )
        page.addView(label("Document category"))
        page.addView(categorySpinner)

        searchInput = EditText(this).apply {
            hint = "Search notes, CATs or past papers"
            isSingleLine = true
            setPadding(14, 8, 14, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val searchWrap = LinearLayout(this).apply {
            setPadding(14, 8, 14, 8)
            addView(searchInput, LinearLayout.LayoutParams(-1, -2))
        }
        page.addView(searchWrap)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 4, 12, 8)
        }
        val refreshButton = Button(this).apply {
            text = "Refresh materials"
            setOnClickListener { loadUniversityDocuments() }
        }
        showOfflineButton = Button(this).apply {
            text = "Offline downloads"
            setOnClickListener {
                offlineMode = !offlineMode
                text = if (offlineMode) "Show online materials" else "Offline downloads"
                if (offlineMode) showOfflineDocuments() else renderDocuments()
            }
        }
        actions.addView(refreshButton, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(showOfflineButton, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(actions)

        statusText = TextView(this).apply {
            text = "Loading documents…"
            textSize = 13f
            setTextColor(0xFF475569.toInt())
            setPadding(16, 8, 16, 8)
        }
        page.addView(statusText)

        val scroll = ScrollView(this)
        documentList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 4, 12, 24)
        }
        scroll.addView(documentList)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(page)

        universitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (::statusText.isInitialized) loadUniversityDocuments()
            }
        }
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                renderDocuments()
            }
        }
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderDocuments()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun label(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 13f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(0xFF064E3B.toInt())
        setPadding(16, 12, 16, 4)
    }

    private fun loadUniversityDocuments() {
        offlineMode = false
        showOfflineButton.text = "Offline downloads"
        val university = universitySpinner.selectedItem?.toString() ?: sources.keys.first()
        val base = sources[university] ?: return
        statusText.text = "Connecting to $university Google Drive…"
        documentList.removeAllViews()

        executor.execute {
            try {
                val categoryResponse = getJson("$base?action=categories")
                val categoryObject = categoryResponse.optJSONObject("categories")
                    ?: throw IllegalStateException("The Drive service did not return categories.")
                val found = mutableListOf<StudyDocument>()
                val visited = mutableSetOf<String>()

                val jobs = ArrayDeque<Triple<String, String, Int>>()
                val keys = categoryObject.keys()
                while (keys.hasNext()) {
                    val categoryName = keys.next()
                    val folderId = categoryObject.optString(categoryName)
                    if (folderId.isNotBlank()) {
                        jobs.add(Triple(folderId, normalizeCategory(categoryName), 0))
                    }
                }

                var foldersRead = 0
                while (jobs.isNotEmpty() && foldersRead < 2000) {
                    val (folderId, category, depth) = jobs.removeFirst()
                    if (!visited.add(folderId)) continue
                    foldersRead++
                    val response = getJson("$base?folderId=${URLEncoder.encode(folderId, "UTF-8")}")
                    val items = response.optJSONArray("items") ?: continue

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val name = item.optString("name", "Untitled document")
                        val mime = item.optString("mimeType")
                        if (id.isBlank()) continue

                        if (mime == "application/vnd.google-apps.folder") {
                            if (depth < 8) jobs.add(Triple(id, category, depth + 1))
                        } else {
                            found.add(
                                StudyDocument(
                                    id = id,
                                    name = name,
                                    category = category,
                                    folderPath = "",
                                    university = university
                                )
                            )
                        }
                    }
                    runOnUiThread {
                        statusText.text = "Reading Drive folders… $foldersRead folders checked, ${found.size} documents found."
                    }
                }

                allDocuments = found.distinctBy { it.id }
                runOnUiThread {
                    statusText.text = "${allDocuments.size} documents found in $university."
                    renderDocuments()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Could not load materials: ${e.message ?: "Check your internet connection."}"
                    documentList.removeAllViews()
                    addMessage("Please check your internet connection and try Refresh materials. If this continues, the Google Drive listing service may need checking.")
                }
            }
        }
    }

    private fun getJson(address: String): JSONObject {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("Server returned HTTP $code")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeCategory(raw: String): String {
        val s = raw.lowercase(Locale.ROOT).replace(Regex("[^a-z]"), "")
        return when {
            s.matches(Regex("cats?")) || s.startsWith("continuousassessment") -> "CATs"
            s == "note" || s == "notes" -> "Notes"
            s.matches(Regex("pastpapers?")) || s == "papers" || s == "exams" -> "Past Papers"
            else -> "Reference Books"
        }
    }

    private fun renderDocuments() {
        if (!::documentList.isInitialized || offlineMode) return
        documentList.removeAllViews()

        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "All categories"
        val query = searchInput.text?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val filtered = allDocuments.filter { doc ->
            (selectedCategory == "All categories" || doc.category == selectedCategory) &&
                (query.isEmpty() || doc.name.lowercase(Locale.ROOT).contains(query))
        }

        if (filtered.isEmpty()) {
            addMessage("No documents match your search. Try another category or search term.")
            return
        }

        filtered.forEach { doc -> addDocumentCard(doc) }
    }

    private fun addDocumentCard(doc: StudyDocument) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val title = TextView(this).apply {
            text = doc.name
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF172B23.toInt())
        }
        val meta = TextView(this).apply {
            text = "${doc.category}  •  ${doc.university}"
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            setPadding(0, 5, 0, 8)
        }
        val download = Button(this).apply {
            text = "Download for offline use"
            setOnClickListener { downloadDocument(doc) }
        }
        card.addView(title)
        card.addView(meta)
        card.addView(download)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 0, 0, 12)
        documentList.addView(card, params)
    }

    private fun downloadDocument(doc: StudyDocument) {
        try {
            val directory = getOfflineDirectory()
            val safeName = doc.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "StudySprint_document" }
            val filename = if (safeName.lowercase(Locale.ROOT).endsWith(".pdf")) safeName else "$safeName.pdf"
            val destination = File(directory, filename)
            if (destination.exists()) {
                Toast.makeText(this, "Already saved offline: $filename", Toast.LENGTH_LONG).show()
                return
            }

            val downloadUrl = "https://drive.google.com/uc?export=download&id=${URLEncoder.encode(doc.id, "UTF-8")}"
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(doc.name)
                .setDescription("Downloading to StudySprint for offline reading")
                .setMimeType("application/pdf")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Download started. Check Offline downloads when complete.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getOfflineDirectory(): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IllegalStateException("Phone storage is unavailable")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun showOfflineDocuments() {
        documentList.removeAllViews()
        val files = getOfflineDirectory().listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            ?: emptyList()

        statusText.text = "${files.size} files saved on this phone. They remain available without internet."
        if (files.isEmpty()) {
            addMessage("You have not downloaded any documents yet. Find a document online and tap Download for offline use.")
            return
        }

        files.forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val title = TextView(this).apply {
                text = file.name
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF172B23.toInt())
            }
            val size = TextView(this).apply {
                text = String.format(Locale.getDefault(), "%.2f MB", file.length() / 1024.0 / 1024.0)
                textSize = 12f
                setTextColor(0xFF64748B.toInt())
            }
            val open = Button(this).apply {
                text = "Open offline"
                setOnClickListener { openOfflineFile(file) }
            }
            val delete = Button(this).apply {
                text = "Delete download"
                setOnClickListener {
                    file.delete()
                    showOfflineDocuments()
                }
            }
            card.addView(title)
            card.addView(size)
            card.addView(open)
            card.addView(delete)
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 0, 0, 12)
            documentList.addView(card, params)
        }
    }

    private fun openOfflineFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open document with"))
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF reader is installed, or this file is not a valid PDF.", Toast.LENGTH_LONG).show()
        }
    }

    private fun addMessage(message: String) {
        val text = TextView(this).apply {
            this.text = message
            textSize = 14f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 30, 24, 30)
        }
        documentList.addView(text)
    }

    override fun onResume() {
        super.onResume()
        if (offlineMode && ::documentList.isInitialized) showOfflineDocuments()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
