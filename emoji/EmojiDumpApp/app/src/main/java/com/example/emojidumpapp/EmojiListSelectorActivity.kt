package com.example.emojidumpapp

import android.app.Activity
import android.app.ListActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import com.example.emojidumpapp.util.ucd.EmojiData

class EmojiListSelectorActivity : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listAdapter = SimpleAdapter(
            this, getItemList(), R.layout.card, arrayOf("title"), intArrayOf(R.id.activityName))
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) =
        startActivity((l.getItemAtPosition(position) as Map<String, Intent>).get("intent"))

    private fun getItemList(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        EMOJI_LIST_FILTER_MAP.keys.forEach {
            result.add(mutableMapOf("title" to it, "intent" to Intent().apply {
                setClassName(packageName, "com.example.emojidumpapp.EmojiListActivity")
                putExtra("filter", it)
            }))
        }
        return result
    }
}