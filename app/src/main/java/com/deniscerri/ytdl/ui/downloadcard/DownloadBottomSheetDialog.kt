package com.deniscerri.ytdl.ui.downloadcard

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.deniscerri.ytdl.R
import com.deniscerri.ytdl.database.enums.DownloadType
import com.deniscerri.ytdl.database.models.DownloadItem
import com.deniscerri.ytdl.database.models.Format
import com.deniscerri.ytdl.database.models.ResultItem
import com.deniscerri.ytdl.database.repository.DownloadRepository
import com.deniscerri.ytdl.database.viewmodel.CommandTemplateViewModel
import com.deniscerri.ytdl.database.viewmodel.DownloadCardViewModel
import com.deniscerri.ytdl.database.viewmodel.DownloadViewModel
import com.deniscerri.ytdl.database.viewmodel.HistoryViewModel
import com.deniscerri.ytdl.database.viewmodel.ResultViewModel
import com.deniscerri.ytdl.receiver.ShareActivity
import com.deniscerri.ytdl.ui.BaseActivity
import com.deniscerri.ytdl.ui.more.cookies.WebViewActivity
import com.deniscerri.ytdl.util.Extensions.loadThumbnail
import com.deniscerri.ytdl.util.FileUtil
import com.deniscerri.ytdl.util.FormatUtil
import com.deniscerri.ytdl.util.UiUtil
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL


data class SnaptubeFormatOption(
    val format: Format,
    val type: DownloadType,
    val title: String,
    val sizeText: String,
    val container: String
)

class DownloadBottomSheetDialog : BottomSheetDialogFragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentAdapter : DownloadFragmentAdapter
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var downloadCardViewModel: DownloadCardViewModel
    private lateinit var behavior: BottomSheetBehavior<View>
    private lateinit var commandTemplateViewModel : CommandTemplateViewModel
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var updateItem : Button
    private lateinit var view: View
    private lateinit var shimmerLoading :ShimmerFrameLayout
    private lateinit var title : View
    private lateinit var shimmerLoadingSubtitle : ShimmerFrameLayout
    private lateinit var subtitle : View
    private lateinit var parentActivity: BaseActivity

    private var selectedSnaptubeFormat: SnaptubeFormatOption? = null
    private val snaptubeRowViews = mutableListOf<Pair<SnaptubeFormatOption, View>>()
    private lateinit var snaptubeShimmer: ShimmerFrameLayout
    private lateinit var snaptubeFormatsContent: LinearLayout
    private lateinit var btnSnaptubeDownload: Button

    private lateinit var result: ResultItem
    private lateinit var type: DownloadType
    private var ignoreDuplicates: Boolean = false
    private var disableUpdateData : Boolean = false
    private var currentDownloadItem: DownloadItem? = null
    private var incognito: Boolean = false
    private var isAudioOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadViewModel = ViewModelProvider(requireActivity())[DownloadViewModel::class.java]
        historyViewModel = ViewModelProvider(requireActivity())[HistoryViewModel::class.java]
        resultViewModel = ViewModelProvider(requireActivity())[ResultViewModel::class.java]
        commandTemplateViewModel = ViewModelProvider(requireActivity())[CommandTemplateViewModel::class.java]
        downloadCardViewModel = ViewModelProvider(requireActivity())[DownloadCardViewModel::class.java]
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val res = downloadCardViewModel.resultItem
        val dwl = downloadCardViewModel.downloadItem

        type = arguments?.getSerializable("type") as DownloadType
        disableUpdateData = arguments?.getBoolean("disableUpdateData") == true
        ignoreDuplicates = arguments?.getBoolean("ignore_duplicates") == true

        if (res == null){
            dismiss()
            return
        }
        result = res
        currentDownloadItem = dwl
        incognito = currentDownloadItem?.incognito ?: sharedPreferences.getBoolean("incognito", false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val downloadItem = getDownloadItem()
        downloadCardViewModel.setResultItem(result)
        downloadCardViewModel.setDownloadItem(downloadItem)
        arguments?.putSerializable("type", downloadItem.type)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dlg ->
            val bottomSheet = dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val marginPx = (16 * displayMetrics.density).toInt()
                val maxWidthPx = (480 * displayMetrics.density).toInt()

                val targetWidth = minOf(screenWidth - (marginPx * 2), maxWidthPx)
                val horizontalMargin = (screenWidth - targetWidth) / 2

                val layoutParams = sheet.layoutParams
                if (layoutParams is ViewGroup.MarginLayoutParams) {
                    layoutParams.width = targetWidth
                    layoutParams.leftMargin = horizontalMargin
                    layoutParams.rightMargin = horizontalMargin
                    layoutParams.bottomMargin = marginPx
                    if (layoutParams is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                        layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    }
                    sheet.layoutParams = layoutParams
                }
                sheet.requestLayout()
            }
        }
    }

    @SuppressLint("RestrictedApi", "InflateParams")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        view = LayoutInflater.from(context).inflate(R.layout.download_bottom_sheet, null)
        dialog.setContentView(view)
        dialog.window?.navigationBarColor = SurfaceColors.SURFACE_1.getColor(requireActivity())
        parentActivity = activity as BaseActivity

        dialog.setOnShowListener {
            behavior = BottomSheetBehavior.from(view.parent as View)
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            if(resources.getBoolean(R.bool.isTablet) || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = displayMetrics.heightPixels
            }
        }

        tabLayout = view.findViewById(R.id.download_tablayout)
        viewPager2 = view.findViewById(R.id.download_viewpager)
        updateItem = view.findViewById(R.id.update_item)
        viewPager2.isUserInputEnabled = sharedPreferences.getBoolean("swipe_gestures_download_card", true)

        //loading shimmers
        shimmerLoading = view.findViewById(R.id.shimmer_loading_title)
        title = view.findViewById(R.id.bottom_sheet_title)
        shimmerLoadingSubtitle = view.findViewById(R.id.shimmer_loading_subtitle)
        subtitle = view.findViewById(R.id.bottom_sheet_subtitle)

        shimmerLoading.setOnClickListener {
            lifecycleScope.launch {
                resultViewModel.cancelUpdateItemData()
                (updateItem.parent as LinearLayout).visibility = View.VISIBLE
            }
        }

        snaptubeShimmer = view.findViewById(R.id.snaptube_shimmer_formats)
        snaptubeFormatsContent = view.findViewById(R.id.snaptube_formats_content)
        btnSnaptubeDownload = view.findViewById(R.id.btn_snaptube_download)
        setupSnaptubeUI(view)


        (viewPager2.getChildAt(0) as? RecyclerView)?.apply {
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        //check if the item has formats and its audio-only
        val formats = result.formats
        isAudioOnly = formats.isNotEmpty() && formats.none { !it.format_note.contains("audio", ignoreCase = true) }
        if (isAudioOnly){
            (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.isClickable = false
            (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.alpha = 0.3f
        }

        //remove outdated player url of 1hr so it can refetch it in the cut player
        if (result.creationTime > System.currentTimeMillis() - 3600000) result.urls = ""
        val fragmentManager = parentFragmentManager
        fragmentAdapter = DownloadFragmentAdapter(
            fragmentManager,
            lifecycle,
            result,
            currentDownloadItem,
            nonSpecific = result.url.endsWith(".txt"),
            isIncognito = incognito
        )

        viewPager2.adapter = fragmentAdapter
        viewPager2.isSaveFromParentEnabled = false

        view.post {
            when(type) {
                DownloadType.audio -> {
                    tabLayout.getTabAt(1)?.select()
                    viewPager2.setCurrentItem(1, false)
                }
                else -> {
                    if (isAudioOnly){
                        tabLayout.getTabAt(1)?.select()
                        viewPager2.setCurrentItem(1, false)
                        Toast.makeText(context, getString(R.string.audio_only_item), Toast.LENGTH_SHORT).show()
                    }else{
                        tabLayout.getTabAt(0)?.select()
                        viewPager2.setCurrentItem(0, false)
                    }
                }
            }
        }

        sharedPreferences.edit(commit = true) {
            putString("last_used_download_type",
                if (type == DownloadType.audio) DownloadType.audio.toString() else DownloadType.video.toString())
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) return
                if (tab.position == 0 && isAudioOnly){
                    tabLayout.selectTab(tabLayout.getTabAt(1))
                    Toast.makeText(context, getString(R.string.audio_only_item), Toast.LENGTH_SHORT).show()
                } else {
                    viewPager2.setCurrentItem(tab.position, false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        viewPager2.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
                runCatching {
                    sharedPreferences.edit(commit = true) {
                        putString("last_used_download_type",
                            if (position == 1) DownloadType.audio.toString() else DownloadType.video.toString())
                    }
                    fragmentAdapter.updateWhenSwitching(viewPager2.currentItem)
                }
            }
        })

        viewPager2.setPageTransformer(BackgroundToForegroundPageTransformer())

        val shownFields = sharedPreferences.getStringSet("modify_download_card", requireContext().resources.getStringArray(R.array.modify_download_card_values).toSet())!!.toList()

        val scheduleBtn = view.findViewById<MaterialButton>(R.id.bottomsheet_schedule_button)
        scheduleBtn.visibility = if(shownFields.contains("schedule")){
            View.VISIBLE
        }else{
            View.GONE
        }
        val download = view.findViewById<Button>(R.id.bottomsheet_download_button)


        scheduleBtn.setOnClickListener{
            UiUtil.showDatePicker(fragmentManager, sharedPreferences) {
                lifecycleScope.launch {
                    resultViewModel.cancelUpdateItemData()
                    resultViewModel.cancelUpdateFormatsItemData()
                }

                scheduleBtn.isEnabled = false
                download.isEnabled = false
                val item: DownloadItem = getDownloadItem()
                item.status = DownloadRepository.Status.Scheduled.toString()
                item.downloadStartTime = it.timeInMillis
                if (item.videoPreferences.alsoDownloadAsAudio){
                    val itemsToQueue = mutableListOf<DownloadItem>()
                    itemsToQueue.add(item)

                    getAlsoAudioDownloadItem(finished = { audioDownloadItem ->
                        audioDownloadItem.downloadStartTime = it.timeInMillis
                        audioDownloadItem.status = DownloadRepository.Status.Scheduled.toString()
                        itemsToQueue.add(audioDownloadItem)

                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO){
                                downloadViewModel.queueDownloads(itemsToQueue, ignoreDuplicates)
                            }

                            if (result.message.isNotBlank()){
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                            }

                            withContext(Dispatchers.Main){
                                handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                            }
                        }
                    })
                }else{
                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO){
                            downloadViewModel.queueDownloads(listOf(item), ignoreDuplicates)
                        }

                        if (result.message.isNotBlank()){
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }

                        withContext(Dispatchers.Main){
                            handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                        }
                    }
                }

            }
        }
        download!!.setOnClickListener {
            lifecycleScope.launch {
                resultViewModel.cancelUpdateItemData()
                resultViewModel.cancelUpdateFormatsItemData()
                scheduleBtn.isEnabled = false
                download.isEnabled = false
                val item: DownloadItem = getDownloadItem()
                if (item.videoPreferences.alsoDownloadAsAudio){
                    val itemsToQueue = mutableListOf<DownloadItem>()
                    itemsToQueue.add(item)

                    getAlsoAudioDownloadItem(finished = {
                        itemsToQueue.add(it)

                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                downloadViewModel.queueDownloads(itemsToQueue, ignoreDuplicates)
                            }
                            withContext(Dispatchers.Main){
                                handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                            }
                        }
                    })
                }else{
                    val result = withContext(Dispatchers.IO) {
                        downloadViewModel.queueDownloads(listOf(item), ignoreDuplicates)
                    }
                    handleDuplicatesAndDismiss(result.duplicateDownloadIDs)
                }
            }
        }

        download.setOnLongClickListener {
            val dd = MaterialAlertDialogBuilder(requireContext())
            dd.setTitle(getString(R.string.save_for_later))
            dd.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
            dd.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                lifecycleScope.launch(Dispatchers.IO){
                    downloadViewModel.putToSaved(getDownloadItem())
                    dismiss()
                }
            }
            dd.show()
            true
        }

        val link = view.findViewById<Button>(R.id.bottom_sheet_link)
        link.visibility = if(shownFields.contains("url")){
            View.VISIBLE
        }else{
            View.GONE
        }

        if (Patterns.WEB_URL.matcher(result.url).matches()){
            link.text = result.url
            link.setOnClickListener{
                UiUtil.openLinkIntent(requireContext(), result.url)
            }
            link.setOnLongClickListener{
                UiUtil.copyLinkToClipBoard(requireContext(), result.url)
                true
            }

            //if auto-update after the card is open is off
            if (result.title.isEmpty() && currentDownloadItem == null && sharedPreferences.getBoolean("quick_download", false)) {
                (updateItem.parent as LinearLayout).visibility = View.VISIBLE
                updateItem.setOnClickListener {
                    (updateItem.parent as LinearLayout).visibility = View.GONE
                    initUpdateData()
                }
            }else{
                (updateItem.parent as LinearLayout).visibility = View.GONE
            }

        }else{
            link.visibility = View.GONE
            (updateItem.parent as LinearLayout).visibility = View.GONE
        }

        val incognitoBtn = view.findViewById<Button>(R.id.bottomsheet_incognito)
        incognitoBtn.alpha = if (incognito) 1f else 0.3f
        incognitoBtn.setOnClickListener {
            if (incognito) {
                it.alpha = 0.3f
            }else{
                it.alpha = 1f
            }

            incognito = !incognito
            fragmentAdapter.isIncognito = incognito
            val onOff = if (incognito) getString(R.string.ok) else getString(R.string.disabled)
            Snackbar.make(incognitoBtn, "${getString(R.string.incognito)}: $onOff", Snackbar.LENGTH_SHORT).show()
        }


        //update in the background if there is no data
        if (result.title.isEmpty() || result.formats.isEmpty()) {
            initUpdateData()
        } else if (!disableUpdateData) {
            val usingGenericFormatsOrEmpty = result.formats.isEmpty() || result.formats.any { it.format_note.contains("ytdlnisgeneric") }
            if (usingGenericFormatsOrEmpty && sharedPreferences.getBoolean("update_formats", false)){
                initUpdateFormats(result)
            }
        }

        lifecycleScope.launch {
            resultViewModel.uiState.collectLatest { res ->
                if (res.errorMessage != null){
                    kotlin.runCatching {
                        UiUtil.handleNoResults(requireActivity(), res.errorMessage!!,
                            url = result.url,
                            continueAnyway =  true,
                            continued = {},
                            cookieFetch = {
                                val myIntent = Intent(requireContext(), WebViewActivity::class.java)
                                myIntent.putExtra("url", "https://${URL(result.url).host}")
                                cookiesFetchedResultLauncher.launch(myIntent)
                            },
                            closed = {
                                dismiss()
                            }
                        )
                    }

                    resultViewModel.uiState.update {it.copy(errorMessage  = null) }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updatingData.collectLatest { isUpdating ->
                kotlin.runCatching {
                    if (isUpdating){
                        title.visibility = View.GONE
                        subtitle.visibility = View.GONE
                        shimmerLoading.visibility = View.VISIBLE
                        shimmerLoadingSubtitle.visibility = View.VISIBLE
                        shimmerLoading.startShimmer()
                        shimmerLoadingSubtitle.startShimmer()
                        (updateItem.parent as LinearLayout).visibility = View.GONE

                        snaptubeShimmer.visibility = View.VISIBLE
                        snaptubeShimmer.startShimmer()
                        snaptubeFormatsContent.visibility = View.GONE
                        btnSnaptubeDownload.isEnabled = false
                    }else{
                        title.visibility = View.VISIBLE
                        subtitle.visibility = View.VISIBLE
                        shimmerLoading.visibility = View.GONE
                        shimmerLoadingSubtitle.visibility = View.GONE
                        shimmerLoading.stopShimmer()
                        shimmerLoadingSubtitle.stopShimmer()

                        snaptubeShimmer.stopShimmer()
                        snaptubeShimmer.visibility = View.GONE
                        snaptubeFormatsContent.visibility = View.VISIBLE
                        btnSnaptubeDownload.isEnabled = (selectedSnaptubeFormat != null)
                    }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updatingFormats.collectLatest {
                kotlin.runCatching {
                    if (it){
                        delay(500)
                        fragmentAdapter.fragments.filterIsInstance<DownloadVideoFragment>().firstOrNull()?.apply {
                            view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.apply {
                                isVisible = true
                                isClickable = true
                                setOnClickListener {
                                    lifecycleScope.launch {
                                        resultViewModel.cancelUpdateFormatsItemData()
                                    }
                                }
                            }
                        }
                        fragmentAdapter.fragments.filterIsInstance<DownloadAudioFragment>().firstOrNull()?.apply {
                            view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.apply {
                                isVisible = true
                                isClickable = true
                                setOnClickListener {
                                    lifecycleScope.launch {
                                        resultViewModel.cancelUpdateFormatsItemData()
                                    }
                                }
                            }
                        }
                    }else{
                        fragmentAdapter.fragments.filterIsInstance<DownloadVideoFragment>().firstOrNull()?.apply {
                            view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.apply {
                                isVisible = false
                                isClickable = false
                            }
                        }
                        fragmentAdapter.fragments.filterIsInstance<DownloadAudioFragment>().firstOrNull()?.apply {
                            view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.apply {
                                isVisible = false
                                isClickable = false
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updateResultData.collectLatest { result ->
                if (result == null) return@collectLatest
                kotlin.runCatching {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (result.size == 1 && result[0] != null) {
                            val res = result[0]!!
                            this@DownloadBottomSheetDialog.result = res
                            fragmentAdapter.setResultItem(res)
                            populateSnaptubeUI(res)

                            title.visibility = View.VISIBLE
                            subtitle.visibility = View.VISIBLE
                            shimmerLoading.visibility = View.GONE
                            shimmerLoadingSubtitle.visibility = View.GONE
                            shimmerLoading.stopShimmer()
                            shimmerLoadingSubtitle.stopShimmer()

                            val usingGenericFormatsOrEmpty = res.formats.isEmpty() || res.formats.any { it.format_note.contains("ytdlnisgeneric") }
                            downloadCardViewModel.setResultItem(res)
                            if (usingGenericFormatsOrEmpty && sharedPreferences.getBoolean("update_formats", false)){
                                initUpdateFormats(res)
                            }

                        }else if (result.size > 1) {
                            //open multi download card instead
                            if (activity is ShareActivity){
                                findNavController().navigate(R.id.action_downloadBottomSheetDialog_to_selectPlaylistItemsDialog, bundleOf(
                                    Pair("resultIDs", result.map { it!!.id }.toLongArray()),
                                ))
                            }else{
                                dismiss()
                            }
                        }

                        resultViewModel.updateResultData.emit(null)
                    }

                }
            }
        }

        lifecycleScope.launch {
            launch{
                downloadViewModel.alreadyExistsUiState.collectLatest { res ->
                    if (res.isNotEmpty() && activity is ShareActivity){
                        withContext(Dispatchers.Main){
                            val bundle = bundleOf(
                                Pair("duplicates", ArrayList(res))
                            )
                            delay(500)
                            findNavController().navigate(R.id.action_downloadBottomSheetDialog_to_downloadsAlreadyExistDialog2, bundle)
                        }
                        downloadViewModel.alreadyExistsUiState.value = mutableListOf()
                    }
                }
            }
        }

        lifecycleScope.launch {
            resultViewModel.updateFormatsResultData.collectLatest { formats ->
                if (formats == null) return@collectLatest
                kotlin.runCatching {
                    isAudioOnly = formats.isNotEmpty() && formats.none { !it.format_note.contains("audio") }
                    if (isAudioOnly){
                        (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.isClickable = false
                        (tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(0)?.alpha = 0.3f
                        Toast.makeText(context, getString(R.string.audio_only_item), Toast.LENGTH_SHORT).show()
                        tabLayout.getTabAt(1)!!.select()
                        viewPager2.setCurrentItem(1, false)
                    }

                    lifecycleScope.launch {
                        withContext(Dispatchers.Main){
                            runCatching {
                                val f1 = fragmentAdapter.fragments.filterIsInstance<DownloadAudioFragment>().firstOrNull()
                                    ?: (fragmentAdapter.fragments[1] as DownloadAudioFragment)
                                val resultItem = downloadViewModel.createResultItemFromDownload(f1.downloadItem)
                                resultItem.formats = formats
                                fragmentAdapter.setResultItem(resultItem)
                                f1.view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.visibility = View.GONE
                            }
                            runCatching {
                                val f1 = fragmentAdapter.fragments.filterIsInstance<DownloadVideoFragment>().firstOrNull()
                                    ?: (fragmentAdapter.fragments[0] as DownloadVideoFragment)
                                val resultItem = downloadViewModel.createResultItemFromDownload(f1.downloadItem)
                                resultItem.formats = formats
                                fragmentAdapter.setResultItem(resultItem)
                                f1.view?.findViewById<LinearProgressIndicator>(R.id.format_loading_progress)?.visibility = View.GONE
                            }
                        }

                        if (formats.isNotEmpty()){
                            result.formats = formats
                            withContext(Dispatchers.Main) {
                                populateSnaptubeUI(result)
                            }
                        }
                        resultViewModel.updateFormatsResultData.emit(null)
                    }
                }
            }
        }
    }

    private var cookiesFetchedResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            sharedPreferences.edit().putBoolean("use_cookies", true).apply()
            updateItem.isVisible = true
            initUpdateData()
        }
    }

    private fun getDownloadItem(selectedTabPosition: Int = tabLayout.selectedTabPosition) : DownloadItem {
        return fragmentAdapter.getDownloadItem(selectedTabPosition)
    }

    private fun getAlsoAudioDownloadItem(finished: (it: DownloadItem) -> Unit) {
        try {
            val ff = fragmentAdapter.fragments.filterIsInstance<DownloadAudioFragment>().firstOrNull()
                ?: (fragmentAdapter.fragments[1] as DownloadAudioFragment)
            getDownloadItem(0).videoPreferences.audioFormatIDs.apply {
                if (this.isNotEmpty()) {
                    ff.updateSelectedAudioFormat(this.first())
                }
            }
            finished(ff.downloadItem)
        }catch (e: Exception){
            val fragmentLifecycleCallback = object:
                FragmentManager.FragmentLifecycleCallbacks() {

                override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                    fragmentManager?.unregisterFragmentLifecycleCallbacks(this)
                    val ff = (f as DownloadAudioFragment)
                    ff.requireView().post {
                        ff.updateSelectedAudioFormat(getDownloadItem(0).videoPreferences.audioFormatIDs.first())
                        finished(ff.downloadItem)
                    }
                    super.onFragmentStarted(fm, f)
                }


            }

            fragmentManager?.registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true)
            viewPager2.setCurrentItem(1, true)
        }
    }

    private fun initUpdateData() {
        kotlin.runCatching {
            if (result.url.isBlank()) {
                dismiss()
                return
            }
            if (resultViewModel.updatingData.value) return

            lifecycleScope.launch(Dispatchers.IO) {
                resultViewModel.updateItemData(result)
            }
        }
    }

    private fun initUpdateFormats(res: ResultItem){
        kotlin.runCatching {
            if (resultViewModel.updatingFormats.value) return
            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                resultViewModel.updateFormatItemData(res)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        lifecycleScope.launch {
            resultViewModel.cancelUpdateItemData()
            resultViewModel.cancelUpdateFormatsItemData()
            super.onDismiss(dialog)
        }
    }

    private fun handleDuplicatesAndDismiss(res: List<DownloadViewModel.AlreadyExistsIDs>) {
        lifecycleScope.launch(Dispatchers.IO) {
            resultViewModel.deleteAll()
        }
        if (activity is ShareActivity && res.isNotEmpty()) {
            //let the lifecycle listener handle it
        }else{
            dismiss()
        }
    }

    private fun setupSnaptubeUI(v: View) {
        val rowMore = v.findViewById<View>(R.id.row_more_formats)
        val tvAll = v.findViewById<TextView>(R.id.tv_more_formats_all)
        val containerExtra = v.findViewById<LinearLayout>(R.id.container_extra_formats)

        rowMore?.setOnClickListener {
            if (containerExtra?.visibility == View.VISIBLE) {
                containerExtra.visibility = View.GONE
                tvAll?.text = "All  >"
            } else {
                containerExtra?.visibility = View.VISIBLE
                tvAll?.text = "Less  ^"
            }
        }

        btnSnaptubeDownload.setOnClickListener {
            val selected = selectedSnaptubeFormat ?: return@setOnClickListener
            btnSnaptubeDownload.isEnabled = false

            lifecycleScope.launch {
                resultViewModel.cancelUpdateItemData()
                resultViewModel.cancelUpdateFormatsItemData()

                val downloadItem = withContext(Dispatchers.IO) {
                    val chosenType = selected.type
                    val item = downloadViewModel.createDownloadItemFromResult(result, result.url, chosenType)
                    item.format = selected.format
                    item.container = selected.container.ifEmpty {
                        if (chosenType == DownloadType.audio) "mp3" else "mp4"
                    }
                    if (chosenType == DownloadType.video) {
                        if (selected.format.acodec == "none" || selected.format.acodec.isBlank()) {
                            item.videoPreferences.audioFormatIDs = downloadViewModel.getPreferredAudioFormats(result.formats)
                        }
                    }
                    item.incognito = incognito
                    item
                }

                val queueResult = withContext(Dispatchers.IO) {
                    downloadViewModel.queueDownloads(listOf(downloadItem), ignoreDuplicates)
                }

                if (queueResult.message.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), queueResult.message, Toast.LENGTH_LONG).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    handleDuplicatesAndDismiss(queueResult.duplicateDownloadIDs)
                }
            }
        }

        btnSnaptubeDownload.setOnLongClickListener {
            val selected = selectedSnaptubeFormat ?: return@setOnLongClickListener false
            val dd = MaterialAlertDialogBuilder(requireContext())
            dd.setTitle(getString(R.string.save_for_later))
            dd.setNegativeButton(getString(R.string.cancel)) { d, _ -> d.cancel() }
            dd.setPositiveButton(getString(R.string.ok)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val item = downloadViewModel.createDownloadItemFromResult(result, result.url, selected.type)
                    item.format = selected.format
                    item.container = selected.container.ifEmpty { if (selected.type == DownloadType.audio) "mp3" else "mp4" }
                    item.incognito = incognito
                    downloadViewModel.putToSaved(item)
                    withContext(Dispatchers.Main) { dismiss() }
                }
            }
            dd.show()
            true
        }

        if (result.title.isNotEmpty()) {
            populateSnaptubeUI(result)
        } else {
            val tvTitle = v.findViewById<TextView>(R.id.snaptube_video_title)
            val tvSource = v.findViewById<TextView>(R.id.snaptube_video_source)
            tvTitle?.text = "Loading video details..."
            tvSource?.text = result.url.ifEmpty { "Please wait..." }
            snaptubeShimmer.visibility = View.VISIBLE
            snaptubeShimmer.startShimmer()
            snaptubeFormatsContent.visibility = View.GONE
            btnSnaptubeDownload.isEnabled = false
        }
    }

    private fun populateSnaptubeUI(res: ResultItem) {
        if (!::view.isInitialized) return
        val tvTitle = view.findViewById<TextView>(R.id.snaptube_video_title)
        val tvSource = view.findViewById<TextView>(R.id.snaptube_video_source)
        val ivThumb = view.findViewById<ImageView>(R.id.snaptube_video_thumbnail)

        tvTitle?.text = res.title.ifEmpty { res.url }
        val author = res.author.ifEmpty { runCatching { URL(res.url).host }.getOrDefault("") }
        val durationText = if (res.duration.isNotBlank() && res.duration != "0:00" && res.duration != "-1") " • ${res.duration}" else ""
        tvSource?.text = if (author.isNotEmpty()) "$author$durationText" else res.url

        ivThumb?.loadThumbnail(hideThumb = false, imageURL = res.thumb)

        snaptubeShimmer.stopShimmer()
        snaptubeShimmer.visibility = View.GONE
        snaptubeFormatsContent.visibility = View.VISIBLE

        val musicContainer = view.findViewById<LinearLayout>(R.id.container_music_formats) ?: return
        val videoContainer = view.findViewById<LinearLayout>(R.id.container_video_formats) ?: return
        val extraContainer = view.findViewById<LinearLayout>(R.id.container_extra_formats) ?: return
        val rowMore = view.findViewById<View>(R.id.row_more_formats) ?: return
        val sectionVideo = view.findViewById<TextView>(R.id.snaptube_section_video)

        musicContainer.removeAllViews()
        videoContainer.removeAllViews()
        extraContainer.removeAllViews()
        snaptubeRowViews.clear()

        val formatUtil = FormatUtil(requireContext())
        val allFormats = res.formats.ifEmpty {
            val generic = mutableListOf<Format>()
            generic.addAll(formatUtil.getGenericVideoFormats(resources))
            generic.addAll(formatUtil.getGenericAudioFormats(resources))
            generic
        }

        val audioFormats = allFormats.filter {
            it.vcodec.isBlank() || it.vcodec == "none" || it.format_note.contains("audio", ignoreCase = true)
        }

        val videoFormats = allFormats.filter {
            (it.vcodec.isNotBlank() && it.vcodec != "none") ||
            (it.height != null && it.height!! > 0) ||
            it.format_note.contains("p", ignoreCase = true)
        }

        // Music Formats
        val musicOptions = mutableListOf<SnaptubeFormatOption>()
        val sortedAudio = audioFormats.sortedWith(
            compareByDescending<Format> { it.filesize }
                .thenByDescending { it.tbr?.toFloatOrNull() ?: 0f }
        )

        val bestAudio = sortedAudio.firstOrNull() ?: formatUtil.getGenericAudioFormats(resources).first()
        val mp3Size = if (bestAudio.filesize > 0) FileUtil.convertFileSize(bestAudio.filesize) else ""
        musicOptions.add(
            SnaptubeFormatOption(
                format = bestAudio,
                type = DownloadType.audio,
                title = "MP3 (Classic)",
                sizeText = mp3Size,
                container = "mp3"
            )
        )

        val m4aAudio = sortedAudio.find { it.container.equals("m4a", ignoreCase = true) }
            ?: sortedAudio.getOrNull(1)
            ?: bestAudio
        val m4aSize = if (m4aAudio.filesize > 0) FileUtil.convertFileSize(m4aAudio.filesize) else ""
        musicOptions.add(
            SnaptubeFormatOption(
                format = m4aAudio,
                type = DownloadType.audio,
                title = "M4A (Audio)",
                sizeText = m4aSize,
                container = "m4a"
            )
        )

        for (opt in musicOptions) {
            val row = createFormatRowView(opt, musicContainer)
            musicContainer.addView(row)
            snaptubeRowViews.add(Pair(opt, row))
        }

        // Video Formats
        val videoOptions = mutableListOf<SnaptubeFormatOption>()
        val extraOptions = mutableListOf<SnaptubeFormatOption>()

        if (isAudioOnly) {
            sectionVideo?.visibility = View.GONE
            videoContainer.visibility = View.GONE
            extraContainer.visibility = View.GONE
            rowMore.visibility = View.GONE
        } else {
            sectionVideo?.visibility = View.VISIBLE
            videoContainer.visibility = View.VISIBLE

            if (videoFormats.isNotEmpty()) {
                fun extractHeight(f: Format): Int {
                    if (f.height != null && f.height!! > 0) return f.height!!
                    val regex = "(\\d{3,4})p".toRegex(RegexOption.IGNORE_CASE)
                    val m = regex.find(f.format_note) ?: regex.find(f.format_id)
                    return m?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }

                val grouped = videoFormats.groupBy { extractHeight(it) }
                val heightsDesc = grouped.keys.filter { it > 0 }.sortedDescending()
                val mainHeights = listOf(1080, 720, 480, 360)
                val bestAudioFilesize = sortedAudio.maxOfOrNull { it.filesize } ?: 0L

                for (h in heightsDesc) {
                    val formatsInHeight = grouped[h] ?: continue
                    val best = formatsInHeight.maxWithOrNull(
                        compareBy<Format> { it.container.equals("mp4", ignoreCase = true) }
                            .thenBy { it.filesize }
                            .thenBy { it.tbr?.toFloatOrNull() ?: 0f }
                    ) ?: formatsInHeight.first()

                    var totalSize = best.filesize
                    if ((best.acodec.isBlank() || best.acodec == "none") && bestAudioFilesize > 0) {
                        if (totalSize > 0) totalSize += bestAudioFilesize
                    }
                    val sizeText = if (totalSize > 0) FileUtil.convertFileSize(totalSize) else ""

                    val title = when (h) {
                        2160 -> "4K (2160p)"
                        1440 -> "2K (1440p)"
                        1080 -> "1080p HD"
                        720 -> "720p HD"
                        480 -> "480p"
                        360 -> "360p"
                        240 -> "240p"
                        144 -> "144p"
                        else -> "${h}p"
                    }

                    val opt = SnaptubeFormatOption(
                        format = best,
                        type = DownloadType.video,
                        title = title,
                        sizeText = sizeText,
                        container = "mp4"
                    )

                    if (mainHeights.contains(h)) {
                        videoOptions.add(opt)
                    } else {
                        extraOptions.add(opt)
                    }
                }

                if (videoOptions.isEmpty() && extraOptions.isNotEmpty()) {
                    videoOptions.addAll(extraOptions.take(4))
                    extraOptions.removeAll(videoOptions)
                }
            } else {
                val genericVideos = formatUtil.getGenericVideoFormats(resources)
                val standardRes = listOf("1080p HD", "720p HD", "480p", "360p")
                genericVideos.take(4).forEachIndexed { idx, f ->
                    videoOptions.add(
                        SnaptubeFormatOption(
                            format = f,
                            type = DownloadType.video,
                            title = standardRes.getOrNull(idx) ?: f.format_note,
                            sizeText = "",
                            container = "mp4"
                        )
                    )
                }
            }

            for (opt in videoOptions) {
                val row = createFormatRowView(opt, videoContainer)
                videoContainer.addView(row)
                snaptubeRowViews.add(Pair(opt, row))
            }

            for (opt in extraOptions) {
                val row = createFormatRowView(opt, extraContainer)
                extraContainer.addView(row)
                snaptubeRowViews.add(Pair(opt, row))
            }

            rowMore.visibility = if (extraOptions.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Default selection: 720p -> 1080p -> first video -> first audio
        selectedSnaptubeFormat = videoOptions.find { it.title.contains("720") }
            ?: videoOptions.find { it.title.contains("1080") }
            ?: videoOptions.firstOrNull()
            ?: musicOptions.firstOrNull()

        updateSnaptubeRadioStates()
    }

    private fun createFormatRowView(opt: SnaptubeFormatOption, parent: ViewGroup): View {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_snaptube_format, parent, false)
        val ivIcon = row.findViewById<ImageView>(R.id.iv_format_icon)
        val tvTitle = row.findViewById<TextView>(R.id.tv_format_title)
        val tvSize = row.findViewById<TextView>(R.id.tv_format_size)

        ivIcon?.setImageResource(if (opt.type == DownloadType.audio) R.drawable.ic_snaptube_music else R.drawable.ic_snaptube_video)
        tvTitle?.text = opt.title
        tvSize?.text = opt.sizeText

        row.setOnClickListener {
            selectedSnaptubeFormat = opt
            updateSnaptubeRadioStates()
        }
        return row
    }

    private fun updateSnaptubeRadioStates() {
        for ((opt, row) in snaptubeRowViews) {
            val ivRadio = row.findViewById<ImageView>(R.id.iv_format_radio)
            val isSelected = (opt == selectedSnaptubeFormat)
            ivRadio?.setImageResource(
                if (isSelected) R.drawable.ic_snaptube_radio_checked
                else R.drawable.ic_snaptube_radio_unchecked
            )
        }
        btnSnaptubeDownload.isEnabled = (selectedSnaptubeFormat != null)
    }
}

