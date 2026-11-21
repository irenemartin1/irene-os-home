package app.irene.pastel

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

/** Health Connect shows this when you ask "why does Irene OS want this?" */
class HealthPrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#07040c"))
            addView(TextView(this@HealthPrivacyActivity).apply {
                setPadding(pad, pad * 2, pad, pad)
                setTextColor(Color.parseColor("#ece6f7"))
                textSize = 16f
                text = "Irene OS ♡ health\n\n" +
                    "The Irene OS home screen reads your steps, sleep, weight, resting heart rate, blood pressure, " +
                    "water and body fat from Health Connect, and sends a daily summary of the last 7 days to your own " +
                    "Irene OS (ireneos.app), where only you can see it under Actions → Habits → Health.\n\n" +
                    "It only reads — it never writes or deletes anything in Health Connect — and nothing is shared with anyone else.\n\n" +
                    "To stop: remove the key in the home screen menu (health sync), turn off health sync in Irene OS Settings, " +
                    "or revoke access in Health Connect."
            })
        })
    }
}
