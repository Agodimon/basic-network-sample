package com.example.basic_network_sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basic_network_sample.App
import com.example.basic_network_sample.R
import com.example.basic_network_sample.databinding.FragmentCoroutinesBinding
import com.example.basic_network_sample.network.models.PhotoResponse
import com.example.basic_network_sample.utils.hide
import com.example.basic_network_sample.utils.network_state_manager.NetworkStateManager
import com.example.basic_network_sample.utils.network_state_manager.NetworkStateManagerImpl
import com.example.basic_network_sample.utils.show
import com.example.basic_network_sample.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoroutinesFragment : Fragment(), NetworkStateManager.OnNetworkStateChangeListener {

  private var _binding: FragmentCoroutinesBinding? = null

  // This property is only valid between onCreateView and onDestroyView.
  private val binding get() = _binding!!
  private var networkStateManager: NetworkStateManager? = null
  private val photoAdapter = PhotoAdapter()
  private var hasData: Boolean = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCoroutinesBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    init()
    requestData()
  }

  private fun init() {
    networkStateManager = context?.let { NetworkStateManagerImpl(it) }
    networkStateManager?.register(this)

    binding.rvPhotos.run {
      layoutManager = LinearLayoutManager(context)
      adapter = photoAdapter
      setHasFixedSize(true)
    }
  }

  private fun requestData() {
    if (hasData) return

    viewLifecycleOwner.lifecycleScope.launch {
      showProgressBar(isShow = true)

      try {
        val photos: List<PhotoResponse> = withContext(Dispatchers.IO) {
          App.instance.apiService.getPhotosCoroutines().photos
        }
        processRequestResult(isSuccess = true)
        photoAdapter.addPhotos(photos)
      } catch (e: Exception) {
        processRequestResult(isSuccess = false)
      }
    }
  }

  private fun showProgressBar(isShow: Boolean) {
    activity?.runOnUiThread {
      if (isShow) binding.progressBar.show() else binding.progressBar.hide()
    }
  }

  private fun processRequestResult(isSuccess: Boolean) {
    showProgressBar(isShow = false)

    if (isSuccess) {
      hasData = true
      toast(R.string.success)
      binding.tvStub.hide()
      binding.rvPhotos.show()
    } else {
      toast(R.string.error)
      binding.rvPhotos.hide()
      binding.tvStub.show()
    }
  }

  override fun onNetworkStateChanged(isConnected: Boolean) {
    if (isConnected) requestData()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    networkStateManager?.unregister()
    _binding = null
  }
}