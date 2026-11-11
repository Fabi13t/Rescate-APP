package com.rescate.app.util

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.rescate.app.R

 

 
fun View.fadeInUp(delay: Long = 0, distance: Float = 24f, duration: Long = 420) {
    alpha = 0f
    translationY = distance * resources.displayMetrics.density
    isVisible = true
    animate().alpha(1f).translationY(0f)
        .setStartDelay(delay)
        .setDuration(duration)
        .setInterpolator(PathInterpolator(0.4f, 0f, 0.2f, 1f))
        .start()
}

 
fun staggerIn(vararg views: View, startDelay: Long = 0, step: Long = 70) {
    views.forEachIndexed { i, v -> v.fadeInUp(startDelay + i * step) }
}

 
fun View.pop() {
    animate().cancel()
    scaleX = 0.7f; scaleY = 0.7f
    animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(OvershootInterpolator(3f)).start()
}

 
fun View.pulse() {
    animate().scaleX(1.06f).scaleY(1.06f).setDuration(260).withEndAction {
        animate().scaleX(1f).scaleY(1f).setDuration(260).start()
    }.start()
}

 
fun View.shake() = startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))

 
fun TextView.countUp(target: Double, format: (Double) -> String, duration: Long = 1100) {
    ValueAnimator.ofFloat(0f, target.toFloat()).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator(1.6f)
        addUpdateListener { text = format((it.animatedValue as Float).toDouble()) }
        start()
    }
}

fun View.fadeVisible(visible: Boolean, duration: Long = 200) {
    if (visible == isVisible && alpha == 1f) return
    if (visible) {
        alpha = 0f; isVisible = true
        animate().alpha(1f).setDuration(duration).start()
    } else {
        animate().alpha(0f).setDuration(duration).withEndAction { isVisible = false; alpha = 1f }.start()
    }
}

 
fun Fragment.snack(message: String, long: Boolean = false) {
    view?.let { Snackbar.make(it, message, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show() }
}

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    view?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
}

fun Context.shareText(text: String, title: String = "Compartir") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

fun Context.copyToClipboard(label: String, text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

 
fun Context.openDirections(lat: Double, lon: Double, label: String) {
    val geo = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon(${android.net.Uri.encode(label)})"))
    try {
        startActivity(geo)
    } catch (e: android.content.ActivityNotFoundException) {
        val web = Intent(Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=18/$lat/$lon"))
        startActivity(web)
    }
}
