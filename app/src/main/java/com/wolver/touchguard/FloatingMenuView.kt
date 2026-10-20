package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

@SuppressLint("ViewConstructor")
class FloatingMenuView(
    context: Context,
    private val onLockClicked: () -> Unit,
    private val onEditClicked: () -> Unit,
    private val onHideToggleClicked: () -> Unit,
    private val onOpenAppClicked: () -> Unit,
    private val onDismissClicked: () -> Unit
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density

    init {
        // Dim background that dismisses when tapped
        setBackgroundColor(Color.parseColor("#80000000"))
        setOnClickListener { onDismissClicked() }

        // Centered Card container
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = 18f * density
                setStroke((1.5f * density).toInt(), Color.parseColor("#383838"))
            }
            background = bg
            val p = (16 * density).toInt()
            setPadding(p, p, p, p)
            // Prevent clicks on card from dismissing
            setOnClickListener { }
        }

        val cardParams = LayoutParams(
            (260 * density).toInt(),
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // Title
        val tvTitle = TextView(context).apply {
            text = "WOLVER TOUCH GUARD"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (4 * density).toInt())
        }

        val tvSub = TextView(context).apply {
            text = "Quick Control Menu"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (12 * density).toInt())
        }

        card.addView(tvTitle)
        card.addView(tvSub)

        // 1. Lock Mode Button
        val btnLock = createMenuButton(
            text = "🔒  Lock Mode (Silent Guard)",
            bgColor = "#1E1E1E",
            textColor = "#00E676"
        ) {
            onLockClicked()
            onDismissClicked()
        }
        card.addView(btnLock)

        // 2. Edit Mode Button
        val btnEdit = createMenuButton(
            text = "✏️  Edit Zones (Show Bars)",
            bgColor = "#1E1E1E",
            textColor = "#FF5252"
        ) {
            onEditClicked()
            onDismissClicked()
        }
        card.addView(btnEdit)

        // 3. HIDE TOGGLE BUTTON (Prominent Red button as requested)
        val btnHideToggle = createMenuButton(
            text = "🚫  HIDE THIS TOGGLE",
            bgColor = "#FF1744",
            textColor = "#FFFFFF",
            isBold = true
        ) {
            onHideToggleClicked()
            onDismissClicked()
        }
        card.addView(btnHideToggle)

        // 4. Open Settings App
        val btnOpenApp = createMenuButton(
            text = "📱  Open App Settings",
            bgColor = "#1E1E1E",
            textColor = "#CCCCCC"
        ) {
            onOpenAppClicked()
            onDismissClicked()
        }
        card.addView(btnOpenApp)

        // 5. Dismiss / Close Button
        val btnClose = createMenuButton(
            text = "✕  Close Menu",
            bgColor = "#141414",
            textColor = "#777777"
        ) {
            onDismissClicked()
        }
        card.addView(btnClose)

        addView(card, cardParams)
    }

    private fun createMenuButton(
        text: String,
        bgColor: String,
        textColor: String,
        isBold: Boolean = false,
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.parseColor(textColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            isAllCaps = false
            if (isBold) typeface = Typeface.DEFAULT_BOLD

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 10f * density
                setStroke((1f * density).toInt(), Color.parseColor("#333333"))
            }
            background = bg

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (42 * density).toInt()
            ).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
            layoutParams = lp

            setOnClickListener { onClick() }
        }
    }
}
