package moxy.sample.ui

import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult

class ProgressRequestListener(
    private val showProgress: (isProgress: Boolean) -> Unit
) : ImageRequest.Listener {

    override fun onStart(request: ImageRequest) {
        showProgress.invoke(true)
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        showProgress.invoke(false)
    }

    override fun onCancel(request: ImageRequest) {
        showProgress.invoke(false)
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        showProgress.invoke(false)
    }
}
