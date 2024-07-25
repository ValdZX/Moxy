package moxy.sample.dailypicture.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter
import moxy.sample.R
import moxy.sample.dailypicture.bl.view.DailyPictureView
import moxy.sample.dailypicture.bl.DailyPicturePresenter
import moxy.sample.databinding.FragmentDailyPictureBinding
import moxy.sample.ui.ProgressRequestListener
import moxy.sample.ui.ViewBindingHolder
import moxy.sample.ui.openBrowser
import moxy.sample.ui.snackbar
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class DailyPictureFragment : MvpAppCompatFragment(),
    DailyPictureView {

    @Inject
    lateinit var presenterProvider: Provider<DailyPicturePresenter>

    // moxyPresenter delegate is the recommended way to create an instance of Presenter in Kotlin.
    // This is a factory for creating presenter for this fragment. You can do it
    // any way you want: manually, or with DI framework of your choice.
    // We use Dagger Hilt as an example of DI framework integration.
    private val presenter: DailyPicturePresenter by moxyPresenter { presenterProvider.get() }

    private val bindingHolder = ViewBindingHolder<FragmentDailyPictureBinding>()
    private val binding get() = bindingHolder.binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = bindingHolder.createView(viewLifecycleOwner) {
        FragmentDailyPictureBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeRefreshLayout.setOnRefreshListener { presenter.onRefresh() }
        binding.imageDailyPicture.setOnClickListener { presenter.onPictureClicked() }
        binding.buttonRandomize.setOnClickListener { presenter.onRandomizeClicked() }
    }

    override fun showImage(url: String) {
        binding.imageDailyPicture.isVisible = true
        binding.imageDailyPicture.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_image_padded)
            error(R.drawable.ic_placeholder_error_padded)
            listener(ProgressRequestListener { isProgress ->
                binding.progressBar.isVisible = isProgress // TODO business logic
            })
        }

    }

    override fun showVideo() {
        binding.imageDailyPicture.isVisible = true
        binding.imageDailyPicture.setImageResource(R.drawable.ic_placeholder_video_padded)
    }

    override fun hideImage() {
        binding.imageDailyPicture.isVisible = false
    }

    override fun showProgress(isProgress: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isProgress
    }

    override fun openBrowser(url: String) {
        openBrowser(Uri.parse(url)) {
            presenter.onOpenBrowserError()
        }
    }

    override fun showError(message: String) {
        binding.root.snackbar(message)
    }

    override fun setTitle(text: String) {
        binding.textTitle.text = text
    }

    override fun setDescription(text: String) {
        binding.textPictureDescription.text = text
    }

    override fun showCopyright(text: String) {
        binding.textCopyright.isVisible = true
        binding.textCopyright.text = text
    }

    override fun hideCopyright() {
        binding.textCopyright.isVisible = false
    }
}
