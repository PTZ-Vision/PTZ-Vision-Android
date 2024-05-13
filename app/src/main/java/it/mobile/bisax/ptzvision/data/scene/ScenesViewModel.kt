package it.mobile.bisax.ptzvision.data.scene

import android.content.Context
import androidx.lifecycle.ViewModel
import it.mobile.bisax.ptzvision.data.PTZDatabase
import kotlinx.coroutines.flow.Flow

class ScenesViewModel(context: Context) : ViewModel() {
    private val repository: ScenesRepository

    init {
        val sceneDao = PTZDatabase.getDatabase(context).sceneDao()
        repository = ScenesRepository(sceneDao)
    }

    fun getScenesByCam(camId: Int): Flow<List<Scene>> {
        return repository.getScenesByCam(camId)
    }

    suspend fun addScene(scene: Scene) {
        repository.insertScene(scene)
    }

    suspend fun updateScene(scene: Scene) {
        repository.updateScene(scene)
    }
}