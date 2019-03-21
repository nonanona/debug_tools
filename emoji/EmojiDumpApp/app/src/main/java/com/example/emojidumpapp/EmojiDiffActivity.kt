package com.example.emojidumpapp

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.emojidumpapp.util.PrivateTypeface
import com.example.emojidumpapp.util.ucd.EmojiData
import com.example.emojidumpapp.util.ucd.UnicodeEmoji
import kotlinx.android.synthetic.main.emoji_diff.view.*
import kotlinx.android.synthetic.main.emoji_list.*
import java.io.File

class EmojiDiffActivity : AppCompatActivity() {
    class Holder(val view: ViewGroup) : RecyclerView.ViewHolder(view)

    lateinit var mGivenFontTypeface: Typeface
    lateinit var mDeviceFontTypeafce: Typeface
    lateinit var mGivenFontPaint: Paint
    lateinit var mDeviceFontPaint: Paint

    val NO_GLYPH = "\uDB3F\uDFFD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = EMOJI_LIST_FILTER_MAP.get(intent.getStringExtra("filter"))
        if (filter == null) {
            throw RuntimeException("Unknown filter has passed:" + intent.getStringExtra("filter"))
        }

        val fontFile = intent.getStringExtra("fontPath")
        if (fontFile == null) {
            throw RuntimeException("No font file has passed:" + intent.getStringExtra("fontPath"))
        }

        mGivenFontTypeface = PrivateTypeface.buildNoFallbackTypeface(fontFile)
        mDeviceFontTypeafce = PrivateTypeface.buildNoFallbackTypeface("/system/fonts/NotoColorEmoji.ttf")
        mGivenFontPaint = Paint().apply { typeface = mGivenFontTypeface }
        mDeviceFontPaint = Paint().apply { typeface = mDeviceFontTypeafce }

        setContentView(R.layout.emoji_list)

        val filteredEmojis = UnicodeEmoji.getEmojiData(this).filter { filter(it) }

        emojiList.apply {
            layoutManager = GridLayoutManager(this@EmojiDiffActivity, 2)

            adapter = object : RecyclerView.Adapter<Holder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
                    val v = this@EmojiDiffActivity.layoutInflater.inflate(R.layout.emoji_diff, null) as ViewGroup
                    return Holder(v)
                }

                override fun getItemCount(): Int = filteredEmojis.size

                override fun onBindViewHolder(holder: Holder, position: Int) {
                    val emoji = filteredEmojis[position]

                    holder.view.apply {
                        findViewById<TextView>(R.id.deviceFont).apply {
                            if (mDeviceFontPaint.hasGlyph(emoji.str)) {
                                text = emoji.str
                                typeface = mDeviceFontTypeafce
                                setBackgroundColor(Color.WHITE)
                            } else {
                                text = NO_GLYPH
                                typeface = Typeface.DEFAULT
                                setBackgroundColor(Color.GRAY)
                            }

                        }

                        findViewById<TextView>(R.id.givenFont).apply {
                            if (mGivenFontPaint.hasGlyph(emoji.str)) {
                                text = emoji.str
                                typeface = mGivenFontTypeface
                                setBackgroundColor(Color.WHITE)
                            } else {
                                text = NO_GLYPH
                                typeface = Typeface.DEFAULT
                                setBackgroundColor(Color.GRAY)

                            }
                        }

                        findViewById<TextView>(R.id.description).apply {
                            text = emoji.cp.joinToString(separator = " ") { String.format("U+%04X", it) }
                        }
                    }
                }


            }
        }
    }

}
