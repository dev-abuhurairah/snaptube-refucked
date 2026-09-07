package com.deniscerri.ytdl.ui.more

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.deniscerri.ytdl.MainActivity
import com.deniscerri.ytdl.R
import com.deniscerri.ytdl.database.viewmodel.DownloadViewModel
import com.deniscerri.ytdl.ui.more.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class MoreFragment : Fragment() {
    private lateinit var mainSharedPreferences: SharedPreferences
    private lateinit var mainSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var mainActivity: MainActivity
    private lateinit var downloadViewModel: DownloadViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as MainActivity
        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        mainSharedPreferencesEditor = mainSharedPreferences.edit()

        // 1. Download Location
        val locationLayout = view.findViewById<View>(R.id.setting_download_location)
        val tvLocation = view.findViewById<TextView>(R.id.tv_download_location_path)
        val currentPath = mainSharedPreferences.getString("video_path", null)
        if (!currentPath.isNullOrEmpty()) {
            tvLocation.text = currentPath
        } else {
            tvLocation.text = "/Storage/emulated/0/Download/VidSnap"
        }

        locationLayout.setOnClickListener {
            kotlin.runCatching {
                findNavController().navigate(R.id.folderSettingsFragment)
            }.onFailure {
                val intent = Intent(context, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        // 2. Default Quality
        val qualityLayout = view.findViewById<View>(R.id.setting_default_quality)
        val tvQuality = view.findViewById<TextView>(R.id.tv_default_quality_val)
        val savedQuality = mainSharedPreferences.getString("vidsnap_default_quality", "1080p FHD")
        tvQuality.text = savedQuality

        qualityLayout.setOnClickListener {
            val qualities = arrayOf("Best Available", "1080p FHD", "720p HD", "480p", "Audio Only (MP3)")
            val currentIndex = qualities.indexOf(tvQuality.text.toString()).let { if (it >= 0) it else 1 }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Default Quality")
                .setSingleChoiceItems(qualities, currentIndex) { dialog, which ->
                    val selected = qualities[which]
                    tvQuality.text = selected
                    mainSharedPreferencesEditor.putString("vidsnap_default_quality", selected).apply()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // 3. Wi-Fi Only Switch
        val switchWifi = view.findViewById<MaterialSwitch>(R.id.switch_wifi_only)
        switchWifi.isChecked = mainSharedPreferences.getBoolean("wifi_only", false)
        switchWifi.setOnCheckedChangeListener { _, isChecked ->
            mainSharedPreferencesEditor.putBoolean("wifi_only", isChecked).apply()
        }

        // 4. Auto-resume Interrupted Downloads Switch
        val switchAutoResume = view.findViewById<MaterialSwitch>(R.id.switch_auto_resume)
        switchAutoResume.isChecked = mainSharedPreferences.getBoolean("auto_resume", true)
        switchAutoResume.setOnCheckedChangeListener { _, isChecked ->
            mainSharedPreferencesEditor.putBoolean("auto_resume", isChecked).apply()
        }

        // 5. Supported Services
        val servicesLayout = view.findViewById<View>(R.id.setting_supported_services)
        servicesLayout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supported Services")
                .setMessage("VidSnap supports over 1000+ streaming and video platforms powered by yt-dlp:\n\n" +
                        "• YouTube (Videos, Shorts, Playlists, Channels, Audio)\n" +
                        "• Facebook (Public Videos, Reels, Watch)\n" +
                        "• Instagram (Reels, Posts, Stories)\n" +
                        "• TikTok (HD Videos, No Watermark, Audio)\n" +
                        "• Twitter / X (Video posts, Media clips)\n" +
                        "• SoundCloud, Vimeo, Twitch, Reddit, Dailymotion\n" +
                        "• And 1000+ other supported websites worldwide.")
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        // 6. Advanced Settings
        val advancedLayout = view.findViewById<View>(R.id.setting_advanced_settings)
        advancedLayout.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            val tvLocation = v.findViewById<TextView>(R.id.tv_download_location_path)
            val currentPath = mainSharedPreferences.getString("video_path", null)
            if (!currentPath.isNullOrEmpty()) {
                tvLocation.text = currentPath
            }
        }
    }

    companion object {
        const val TAG = "MoreFragment"
    }
}
