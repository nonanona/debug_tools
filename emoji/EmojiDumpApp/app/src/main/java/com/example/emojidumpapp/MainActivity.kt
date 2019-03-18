package com.example.emojidumpapp

import android.app.ListActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter

class MainActivity : ListActivity() {
    private val SAMPLE_CODE_CATEGORY = "com.example.emojidumpapp"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listAdapter = SimpleAdapter(
            this, getItemList(), R.layout.card, arrayOf("title"), intArrayOf(R.id.activityName))
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) =
        startActivity((l.getItemAtPosition(position) as Map<String, Intent>).get("intent"))

    private fun getItemList(): List<Map<String, Any>> {
        val list = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN, null).apply { addCategory(SAMPLE_CODE_CATEGORY) },
            PackageManager.GET_META_DATA) ?: return listOf()

        val result = mutableListOf<Map<String, Any>>()
        list.forEach forEach@{
            // Only collect activities in this apk
            if (it.activityInfo.packageName != packageName) return@forEach

            // Load label. If no label, use activiy name instead
            val label = it.loadLabel(packageManager)?.toString() ?: it.activityInfo.name

            result.add(mutableMapOf("title" to label, "intent" to Intent().apply {
                setClassName(it.activityInfo.applicationInfo.packageName, it.activityInfo.name)
            }))
        }
        return result
    }
}
