package com.example.emojidumpapp.util.ucd

import android.content.Context

class UnicodeEmoji {
    internal companion object {
        private val instance = UnicodeEmoji()

        fun getEmojiData(ctx: Context) : List<EmojiData> {
            if (instance.mEmojiData != null) {
                return instance.mEmojiData!!
            }
            synchronized(instance.lock) {
                if (instance.mEmojiData != null) {
                    return instance.mEmojiData!!
                }
                if (instance.mEmojiData == null) {
                    instance.mEmojiData = UnicodeEmojiDataParser.parse(ctx.assets)
                }
                return instance.mEmojiData!!
            }
        }
    }

    private val lock = Object()
    private var mEmojiData : List<EmojiData>? = null
}