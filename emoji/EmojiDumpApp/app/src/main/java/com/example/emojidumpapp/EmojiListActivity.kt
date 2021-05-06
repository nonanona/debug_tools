package com.example.emojidumpapp

import android.graphics.Color
import android.graphics.Paint
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
    "Emojis hasGlyph false and gen != NA" to { data -> !PAINT.hasGlyph(data.str) && !data.generation.equals("NA")},
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
                        "hasGlyph: ${Paint().hasGlyph(emoji.str)}"
                })
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
                            if (PAINT.hasGlyph(emoji.str)) Color.TRANSPARENT else Color.LTGRAY
                        )
                        setOnLongClickListener {
                            AlertDialog.Builder(this@EmojiListActivity).apply {
                                setView(makeEmojiDetails(emoji))
                                setPositiveButton("OK", null)
                            }.show()
                            return@setOnLongClickListener false
                        }
                    }
                }


            }
        }
    }

}
