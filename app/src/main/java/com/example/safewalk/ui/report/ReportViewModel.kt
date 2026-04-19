package com.example.safewalk.ui.report

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReportViewModel : ViewModel() {
    val selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageData = MutableLiveData<ByteArray?>()
    val selectedLocation = MutableLiveData<String?>()
    val selectedLatitude = MutableLiveData<Double?>()
    val selectedLongitude = MutableLiveData<Double?>()
    val selectedCategory = MutableLiveData<String?>()
    val description = MutableLiveData<String>()
    val otherCategoryText = MutableLiveData<String>()
    val suspectName = MutableLiveData<String>()
}
