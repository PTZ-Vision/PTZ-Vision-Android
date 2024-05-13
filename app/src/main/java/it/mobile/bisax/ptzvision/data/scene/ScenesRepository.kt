package it.mobile.bisax.ptzvision.data.scene

import kotlinx.coroutines.flow.Flow

class ScenesRepository(private val sceneDao: SceneDao) {
    fun getScenesByCam(camId: Int): Flow<List<Scene>> = sceneDao.getScenesbyCam(camId)

    suspend fun insertScene(scene: Scene) = sceneDao.insert(scene)

    suspend fun updateScene(scene: Scene) = sceneDao.update(scene)
}