package it.mobile.bisax.ptzvision.data.cam

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mobile.bisax.ptzvision.data.PTZDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CamsViewModel(context: Context) : ViewModel() {
    val getAllCamsStream: Flow<List<Cam>>
    val getActiveCamsStream: Flow<List<Cam>>
    private val repository: CamsRepository

    init {
        val camDao = PTZDatabase.getDatabase(context).camDao()
        repository = CamsRepository(camDao)
        getAllCamsStream = repository.getAllCamsStream()
        getActiveCamsStream = repository.getActiveCamsStream()
    }

    private fun getCamById(id: Int): Flow<Cam?> {
        return repository.getCamStream(id)
    }

    suspend fun addCam(cam: Cam): Long {
        return repository.insertCam(cam)
    }

    suspend fun updateCam(cam: Cam): Int {
        return repository.updateCam(cam)
    }

    fun removeCam(id: Int){
        viewModelScope.launch(Dispatchers.IO) {
            getAllCamsStream.first().firstOrNull { it.id == id }?.let {
                repository.deleteCam(it)
            }
        }
    }

    suspend fun removeActive(id: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val cam = getCamById(id).first()
            if (cam != null) {
                repository.removeActive(id)
            } else {
                false
            }
        }
    }

    suspend fun addActive(id: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val cam = getCamById(id).first()
            if (cam != null) {
                repository.addActive(id)
            } else {
                false
            }
        }
    }
}