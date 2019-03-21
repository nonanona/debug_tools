package com.example.emojidumpapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.example.emojidumpapp.util.*
import kotlinx.android.synthetic.main.code_point_editor.*
import android.text.TextUtils
import android.view.GestureDetector
import android.view.View
import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.text.method.Touch.onTouchEvent
import android.view.View.OnTouchListener
import android.widget.TextView
import android.widget.Toast


class SyncTextWatcher(val edit: EditText, val syncFunc: () -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        syncFunc()
    }
}

class CodePointInputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if ((end - start) != 1) {
            // Currently don't filter copy-and-paste
            return null
        }

        val input = source.toString().toUpperCase()

        if (input.matches("[\\s]+".toRegex())) {
            // If space is typed, append "U+" for support.
            return " U+"
        }

        if (!input.matches("[\\dA-FU\\+]+".toRegex())) {
            if (input.matches("[U\\+]".toRegex())) {
                return null // Accept "U" and "+" for U+XXXX format
            } else {
                return ""  // Reject otherwise
            }
        }

        if (dest.length == 0) {
            // If this is the first input, prepend "U+"
            return TextUtils.concat("U+", input)
        }

        val codePoints = parseCodePoint(dest.toString())
        if (codePoints.size == 0) {
            // ??? what is happening?
            return null
        }
        val lastCp = codePoints.last()
        val cpCandidate = lastCp * 16 + input.toString().toInt(16)
        if (cpCandidate > 0x10FFFF) {
            // The value exceeds the maximum unicode value. Start new one
            return TextUtils.concat(" U+", input)
        }
        return null // otherwise use original
    }
}

class Utf16InputFilter : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if ((end - start) != 1) {
            // Currently don't filter copy-and-paste
            return null
        }

        val input = source.toString().toUpperCase()

        if (source.matches("[\\s]".toRegex())) {
            // If space is input, replace with "\u".
            if (dest.endsWith("\\u")) {
                return ""
            } else {
                return "\\u"
            }
        }

        if (!input.matches("[\\dA-F]".toRegex())) {
            if (input.matches("[\\\\u]".toRegex())) {
                return null  // accept "\" and "u"
            } else {
                return "" // otherwise reject
            }
        }

        if (dest.length == 0) {
            return TextUtils.concat("\\u", input)
        }

        val utf16s = parseUtf16(dest.toString())
        if (utf16s.size == 0) {
            return null  // ????
        }

        if (utf16s.last().toInt() >= 0xFFF) {
            // Already has 4 chars. insert with new "\u"prefix
            return TextUtils.concat("\\u", input)
        }
        return null
    }
}

class CodePointEditor : Activity() {

    var isSynching = false

    fun syncText(triggeredView: EditText) {
        if (isSynching) {
            return
        }
        isSynching = true
        val rawText: String
        if (triggeredView == rawTextEdit) {
            rawText = rawTextEdit.text.toString()
        } else if (triggeredView == codePointsEdit) {
            rawText = codePointsToString(parseCodePoint(codePointsEdit.text.toString()))
        } else if (triggeredView == utf16UnitsEdit) {
            rawText = utf16ToString(parseUtf16(utf16UnitsEdit.text.toString()))
        } else {
            throw RuntimeException("The syncText is called from unknown EditText")
        }

        if (triggeredView != rawTextEdit) {
            rawTextEdit.setText(rawText)
        }
        if (triggeredView != codePointsEdit) {
            codePointsEdit.setText(toCodePointString(stringToCodePoints(rawText)))
        }
        if (triggeredView != utf16UnitsEdit) {
            utf16UnitsEdit.setText(toUtf16String(stringToUtf16(rawText)))
        }
        isSynching = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.code_point_editor)

        rawTextEdit.addTextChangedListener(SyncTextWatcher(rawTextEdit, { syncText(rawTextEdit) }))
        codePointsEdit.addTextChangedListener(SyncTextWatcher(codePointsEdit, { syncText(codePointsEdit) }))
        utf16UnitsEdit.addTextChangedListener(SyncTextWatcher(utf16UnitsEdit, { syncText(utf16UnitsEdit)}))

        codePointsEdit.filters = arrayOf(CodePointInputFilter())
        utf16UnitsEdit.filters = arrayOf(Utf16InputFilter())

        val codePointClipboardCopier = GestureDetector(this,
                object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip = ClipData.newPlainText("code points", codePointsEdit.text.toString())
                Toast.makeText(this@CodePointEditor, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        codePointsEdit.setOnTouchListener { v, event -> codePointClipboardCopier.onTouchEvent(event) }

        val utf16ClipboardCopier = GestureDetector(this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip = ClipData.newPlainText("code points", utf16UnitsEdit.text.toString())
                        Toast.makeText(this@CodePointEditor, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        return true
                    }
                })
        utf16UnitsEdit.setOnTouchListener { v, event -> utf16ClipboardCopier.onTouchEvent(event)}
    }
}
