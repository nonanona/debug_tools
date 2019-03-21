package com.example.emojidumpapp

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
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

    private fun equalOutput(str: String, tf: Typeface) : Boolean {
        val devicePaint = TextPaint()
        devicePaint.textSize = 64f
        devicePaint.typeface = Typeface.DEFAULT

        val givenPaint = TextPaint();
        givenPaint.textSize = devicePaint.textSize
        givenPaint.typeface = tf

        val deviceLayout = StaticLayout.Builder.obtain(str,0, str.length, devicePaint, Int.MAX_VALUE).build()
        val givenLayout = StaticLayout.Builder.obtain(str, 0, str.length, givenPaint, Int.MAX_VALUE).build()

        if (deviceLayout.height != givenLayout.height)
            return false
        if (deviceLayout.width != givenLayout.width)
            return false

        val deviceBMP = Bitmap.createBitmap(deviceLayout.width, deviceLayout.height, Bitmap.Config.ARGB_8888)
        val deviceCanvas = Canvas(deviceBMP)
        deviceLayout.draw(deviceCanvas)

        val givenBMP = Bitmap.createBitmap(deviceLayout.width, deviceLayout.height, Bitmap.Config.ARGB_8888)
        val givenCanvas = Canvas(givenBMP)
        givenLayout.draw(givenCanvas)

        return deviceBMP.sameAs(givenBMP)
    }

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
                            text = emoji.str + "\uFE0F"
                            typeface = null
                        }

                        findViewById<TextView>(R.id.givenFont).apply {
                            text = emoji.str
                            typeface = mGivenFontTypeface
                        }

                        findViewById<TextView>(R.id.description).apply {
                            text = emoji.cp.joinToString(separator = " ") { String.format("U+%04X", it) }
                        }

                        val diffStr = SpannableString("Different Image").apply {
                            setSpan(ForegroundColorSpan(Color.RED), 0, this.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                        }

                        val sameStr = "No Diff"

                        findViewById<TextView>(R.id.result).apply {
                            text = if (equalOutput(emoji.str + "\uFE0F", mGivenFontTypeface)) {
                                sameStr
                            } else {
                                diffStr
                            }
                        }
                    }
                }


            }
        }
    }

}
