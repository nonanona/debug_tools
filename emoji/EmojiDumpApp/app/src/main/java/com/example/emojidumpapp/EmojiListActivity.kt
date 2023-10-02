package com.example.emojidumpapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.text.TextRunShaper
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.emojidumpapp.util.ucd.EmojiData
import com.example.emojidumpapp.util.ucd.UnicodeEmoji
import kotlinx.android.synthetic.main.emoji_list.*

val PAINT = Paint()

val EMOJI_LIST_FILTER_MAP = mapOf<String, (EmojiData) -> Boolean>(
    "Emoji" to { data -> data.props.contains("Emoji") },
    "Emoji and Emoji_Presentation" to { data -> data.props.contains("Emoji") && data.props.contains("Emoji_Presentation") },
    "Emoji but Not Emoji_Presentation" to { data -> data.props.contains("Emoji") && !data.props.contains("Emoji_Presentation") },
    "Extended_Pictographic" to { data -> data.props.contains("Extended_Pictographic") },
    "Basic Emoji" to { data -> data.props.contains("Basic_Emoji")},
    "Emoji Keycap Sequence" to { data -> data.props.contains("Emoji_Keycap_Sequence")},
    "RGI Emoji Flag Sequence" to { data -> data.props.contains("RGI_Emoji_Flag_Sequence")},
    "RGI Emoji Tag Sequence" to { data -> data.props.contains("RGI_Emoji_Tag_Sequence")},
    "RGI Emoji Modifier Sequence" to { data -> data.props.contains("RGI_Emoji_Modifier_Sequence")},
    "RGI Emoji ZWJ Sequence" to { data -> data.props.contains("RGI_Emoji_ZWJ_Sequence")},
    "Emojis hasGlyph false and gen != NA" to { data -> !PAINT.hasGlyph(data.str) && !data.generation.equals("NA")},
    "Generation E15.0" to { data -> data.generation.equals("E15.0") },
    "Generation E14.0" to { data -> data.generation.equals("E14.0") },
    "Generation E13.1" to { data -> data.generation.equals("E13.1") },
    "Generation E13.0" to { data -> data.generation.equals("E13.0") },
    "Generation E12.0" to { data -> data.generation.equals("E12.0") },
    "Generation E11.0" to { data -> data.generation.equals("E11.0") },
    "Generation E5.0" to { data -> data.generation.equals("E5.0") },
    "Generation E4.0" to { data -> data.generation.equals("E4.0") },
    "Generation E3.0" to { data -> data.generation.equals("E3.0") },
    "Generation E2.0" to { data -> data.generation.equals("E2.0") },
    "Generation E1.0" to { data -> data.generation.equals("E1.0") },
    "Generation E0.7" to { data -> data.generation.equals("E0.7") },
    "Generation E0.6" to { data -> data.generation.equals("E0.6") },
    "Generation E0.0" to { data -> data.generation.equals("E0.0") }

)

class EmojiListActivity : AppCompatActivity() {

    class Holder(val view: AppCompatTextView) : RecyclerView.ViewHolder(view)

    fun makeEmojiDetails(emoji: EmojiData) : View {
        return (this.layoutInflater.inflate(R.layout.emoji_details, null) as ViewGroup).apply {
            findViewById<ViewGroup>(R.id.container).apply {
                findViewById<TextView>(R.id.emojiIcon).text = emoji.str
            }

            findViewById<ViewGroup>(R.id.container2).apply {

                val paint = Paint()

                addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                    findViewById<TextView>(R.id.textView).text =
                        "Sequence: ${emoji.cp.joinToString(separator = ", "){String.format("U+%04X", it)}}"
                })
                addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                    findViewById<TextView>(R.id.textView).text =
                        "Generation: ${emoji.generation}"
                })
                addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                    findViewById<TextView>(R.id.textView).text =
                        "Properties: ${emoji.props.joinToString()}"
                })
                addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                    findViewById<TextView>(R.id.textView).text =
                        "hasGlyph: ${paint.hasGlyph(emoji.str)}"
                })
                if (Build.VERSION.SDK_INT >=31 && paint.hasGlyph(emoji.str)) {
                    val glyphs = TextRunShaper.shapeTextRun(
                        emoji.str, 0, emoji.str.length, 0, emoji.str.length, 0f, 0f, false, paint)

                    addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                        findViewById<TextView>(R.id.textView).text = "Source: " + glyphs.getFont(0).file
                    })

                    addView(this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_details_line, null).apply {
                        findViewById<TextView>(R.id.textView).text = "Glyph ID: " + glyphs.getGlyphId(0)
                    })

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emoji_list)

        val filter = EMOJI_LIST_FILTER_MAP.get(intent.getStringExtra("filter"))
        if (filter == null) {
            throw RuntimeException("Unknown filter has passed:" + intent.getStringExtra("filter"))
        }

        val filteredEmojis = UnicodeEmoji.getEmojiData(this).filter { filter(it) }

        emojiList.apply {
            layoutManager = GridLayoutManager(this@EmojiListActivity, 6)

            adapter = object : RecyclerView.Adapter<Holder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
                    val tv = this@EmojiListActivity.layoutInflater.inflate(R.layout.emoji_card, null) as AppCompatTextView
                    return Holder(tv)
                }

                override fun getItemCount(): Int = filteredEmojis.size

                override fun onBindViewHolder(holder: Holder, position: Int) {
                    val emoji = filteredEmojis[position]

                    holder.view.apply {
                        text = emoji.str
                        setBackgroundColor(
                            if (Build.VERSION.SDK_INT >=23 && !PAINT.hasGlyph(emoji.str)) Color.LTGRAY else Color.TRANSPARENT
                        )
                        setOnLongClickListener {
                            AlertDialog.Builder(this@EmojiListActivity).apply {
                                setView(makeEmojiDetails(emoji))
                                setPositiveButton("OK", null)
                                setNeutralButton("Copy", { dialog, id ->
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("emojoi", emoji.str)
                                    clipboard.setPrimaryClip(clip)
                                })
                            }.show()
                            return@setOnLongClickListener false
                        }
                    }
                }


            }
        }
    }

}
