package com.example.emojidumpapp

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import java.io.File
import java.io.IOException
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog


class EmojiDiffSelectorActivity : ListActivity() {

    private fun findFonts(path: String) : List<File> {
        val targetDir = File("/data/local/tmp/emoji")
        if (!targetDir.exists()) {
            throw IOException("${targetDir.absolutePath} not found.")
        }

        if (!targetDir.isDirectory()) {
            throw IOException("${targetDir.absolutePath} is not a dir")
        }

        if (!targetDir.canExecute()) {
            throw IOException("${targetDir.absolutePath} cannot execute.")
        }

        if (!targetDir.canRead()) {
            throw IOException("${targetDir.absolutePath} cannot read.")
        }

        var fontFiles = targetDir.listFiles()
                .filter { it.isFile }
                .filter { it.canRead() }
                .filter { it.extension == "ttf" || it.extension == "otf" }
                .sortedBy { it.lastModified() }

        if (fontFiles.isEmpty()) {
            throw IOException("No fonts found in ${targetDir.absolutePath}")
        }

        return fontFiles
    }

    var mFontFile : File? = null

    override fun onResume() {
        super.onResume()

        if (mFontFile == null) {
            val adb = AlertDialog.Builder(this)

            var fonts : List<File>
            try {
                fonts = findFonts("/data/local/tmp/emoji")
            } catch (e: IOException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val items = fonts.map { it.name }.toTypedArray()
            adb.setSingleChoiceItems(items, 0) { di, index ->
                mFontFile = fonts[index]
            }
            adb.setPositiveButton("OK") { di, index ->
                if (mFontFile == null) {
                    mFontFile = fonts[0]
                }

                listAdapter = SimpleAdapter(
                        this, getItemList(), R.layout.card, arrayOf("title"), intArrayOf(R.id.activityName))


            }
            adb.setTitle("Which font do you use?")
            adb.show()
            return
        }

    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) =
            startActivity((l.getItemAtPosition(position) as Map<String, Intent>).get("intent"))

    private fun getItemList(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        EMOJI_LIST_FILTER_MAP.keys.forEach {
            result.add(mutableMapOf("title" to it, "intent" to Intent().apply {
                setClassName(packageName, "com.example.emojidumpapp.EmojiDiffActivity")
                putExtra("filter", it)
                putExtra("fontPath", mFontFile?.absolutePath)
            }))
        }
        return result
    }
}
