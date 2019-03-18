package com.example.emojidumpapp

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
    "Emoji_Flag_Sequence" to { data -> data.props.contains("Emoji_Flag_Sequence") },
    "Emoji_Keycap_Sequence" to { data -> data.props.contains("Emoji_Keycap_Sequence") },
    "Emoji_Tag_Sequence" to { data -> data.props.contains("Emoji_Tag_Sequence") },
    "Emoji_Modifier_Sequence" to { data -> data.props.contains("Emoji_Modifier_Sequence") },
    "Emoji_ZWJ_Sequence" to { data -> data.props.contains("Emoji_ZWJ_Sequence") },
    "Emojis hasGlyph false and gen != NA" to { data -> !PAINT.hasGlyph(data.str) && !data.generation.equals("NA")},
    "Generation v12.0" to { data -> data.generation.equals("12.0") },
    "Generation v11.0" to { data -> data.generation.equals("11.0") },
    "Generation v10.0" to { data -> data.generation.equals("10.0") },
    "Generation v9.0" to { data -> data.generation.equals("9.0") },
    "Generation v8.0" to { data -> data.generation.equals("8.0") },
    "Generation v7.0" to { data -> data.generation.equals("7.0") },
    "Generation v6.1" to { data -> data.generation.equals("6.1") },
    "Generation v6.0" to { data -> data.generation.equals("6.0") }
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
            layoutManager = GridLayoutManager(this@EmojiListActivity, 8)

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
