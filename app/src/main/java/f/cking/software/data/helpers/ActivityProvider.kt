package f.cking.software.data.helpers

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity

class ActivityProvider {

    private var activity: AppCompatActivity? = null

    fun setActivity(activity: AppCompatActivity?) {
        this.activity = activity
    }

    fun requireActivity(): Activity = activity ?: throw IllegalStateException("Activity is not initialized")
}