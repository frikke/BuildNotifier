package com.codertainment.buildnotifier.activity

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.NestedScrollView
import br.tiagohm.codeview.CodeView
import br.tiagohm.codeview.Language
import br.tiagohm.codeview.Theme
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.codertainment.buildnotifier.KEY_BUILD_NOTIFICATION
import com.codertainment.buildnotifier.R
import com.codertainment.buildnotifier.model.BuildNotification
import com.droidman.ktoasty.showErrorToast
import com.droidman.ktoasty.showInfoToast
import com.google.firebase.storage.FirebaseStorage
import com.mcxiaoke.koi.log.loge
import io.multimoon.colorful.CAppCompatActivity
import io.multimoon.colorful.Colorful
import kotlinx.android.synthetic.main.activity_logs.*
import kotterknife.bindView
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.imageResource
import java.io.File
import java.text.SimpleDateFormat

class LogsActivity : CAppCompatActivity() {

  companion object {

    fun getIntent(context: Context, notif: BuildNotification): Intent {
      val i = Intent(context, LogsActivity::class.java)
      i.putExtra(KEY_BUILD_NOTIFICATION, notif)
      return i
    }
  }

  private var notif: BuildNotification? = null

  private val icon by bindView<ImageView>(R.id.notification_icon)
  val text by bindView<TextView>(R.id.notification_text)
  val time by bindView<TextView>(R.id.notification_time)
  val device by bindView<TextView>(R.id.notification_device)
  private val step by bindView<TextView>(R.id.notification_step)
  private val buildVersion by bindView<TextView>(R.id.notification_build_version)
  private val timeTaken by bindView<TextView>(R.id.notification_time_taken)

  private val df = SimpleDateFormat("HH:mm\ndd/MM/yyyy")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    notif = intent.getParcelableExtra(KEY_BUILD_NOTIFICATION)
    if (notif == null) {
      loge("Got Null Notif Object")
      finish()
      return
    }

    setContentView(R.layout.activity_logs)

    setSupportActionBar(logs_toolbar)

    title = getString(R.string.title_logs)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    toolbar_layout.setExpandedTitleColor(Color.TRANSPARENT)

    icon.apply {
      imageResource = if (notif!!.status) {
        setBackgroundResource(R.drawable.background_success)
        R.drawable.ic_done_white
      } else {
        setBackgroundResource(R.drawable.background_failure)
        R.drawable.ic_clear_white
      }
    }

    text.text = if (notif!!.status) {
      getString(R.string.build_successful)
    } else {
      getString(R.string.build_failed)
    }

    time.text = df.format(notif!!.time)

    device.text = notif!!.device

    step.text = notif!!.currentStep

    buildVersion.text = notif!!.buildVersion

    timeTaken.text = notif!!.formattedTimeTaken

    logs_details_container.childrenRecursiveSequence().forEach {
      if (it is TextView) {
        if (it.contentDescription != null) {
          if (it.contentDescription.isNotEmpty() && it.contentDescription.isNotBlank()) {
            TooltipCompat.setTooltipText(it, it.contentDescription)
          }
        }
      }
    }

    logs_scroll_view.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
      logs_download_fab.fabTextVisibility = if (scrollY == 0) {
        View.VISIBLE
      } else {
        View.GONE
      }
    }

    val storageFile = FirebaseStorage.getInstance().getReference(notif!!.logFile!!)
    val f = File.createTempFile("log", notif!!.time.toString())
    storageFile.getFile(f).addOnSuccessListener {
      logs_code_view.apply {
        setOnHighlightListener(object : CodeView.OnHighlightListener {
          override fun onStartCodeHighlight() {

          }

          override fun onLanguageDetected(p0: Language?, p1: Int) {

          }

          override fun onFontSizeChanged(p0: Int) {

          }

          override fun onLineClicked(p0: Int, p1: String?) {
            clipboardManager.primaryClip = ClipData.newPlainText("Build Logs", p1)
            showInfoToast(String.format(getString(R.string.line_copied), p0))
          }

          override fun onFinishCodeHighlight() {
            runOnUiThread {
              logs_progress.visibility = View.GONE
              logs_code_view.visibility = View.VISIBLE
            }
          }
        })
        code = f.readText()
        isShowLineNumber = true
        isWrapLine = false
        isZoomEnabled = true
        theme = if (Colorful().getDarkTheme()) Theme.XT256 else Theme.XCODE
        startLineNumber = 1
        language = Language.ACCESS_LOG
        apply()
      }
    }.addOnFailureListener{
      it.printStackTrace()
      showErrorToast(it.localizedMessage)
      logs_progress.visibility = View.GONE
    }
    /*AndroidNetworking.get(notif!!.logFile)
      .build()
      .getAsString(object : StringRequestListener {
        override fun onResponse(response: String?) {
          if (response != null) {
            logs_code_view.apply {
              setOnHighlightListener(object : CodeView.OnHighlightListener {
                override fun onStartCodeHighlight() {

                }

                override fun onLanguageDetected(p0: Language?, p1: Int) {

                }

                override fun onFontSizeChanged(p0: Int) {

                }

                override fun onLineClicked(p0: Int, p1: String?) {
                  clipboardManager.primaryClip = ClipData.newPlainText("Build Logs", p1)
                  showInfoToast(String.format(getString(R.string.line_copied), p0))
                }

                override fun onFinishCodeHighlight() {
                  runOnUiThread {
                    logs_progress.visibility = View.GONE
                    logs_code_view.visibility = View.VISIBLE
                  }
                }
              })
              code = response
              isShowLineNumber = true
              isWrapLine = false
              isZoomEnabled = true
              theme = if (Colorful().getDarkTheme()) Theme.ATOM_ONE_DARK else Theme.ATOM_ONE_LIGHT
              startLineNumber = 1
              language = Language.AUTO
              apply()
            }
          }
        }

        override fun onError(anError: ANError?) {
          anError?.printStackTrace()
          showErrorToast(getString(R.string.failed_to_load_log))
          logs_progress.visibility = View.GONE
        }
      })*/
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}