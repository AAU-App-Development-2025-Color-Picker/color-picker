package at.aau.appdev.colorpicker.detail

import androidx.lifecycle.ViewModel
import at.aau.appdev.colorpicker.persistence.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val repository: Repository) : ViewModel() {}