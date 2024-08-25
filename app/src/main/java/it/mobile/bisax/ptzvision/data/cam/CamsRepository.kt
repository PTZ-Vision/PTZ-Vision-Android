package it.mobile.bisax.ptzvision.data.cam

import kotlinx.coroutines.flow.Flow

class CamsRepository(private val camDao: CamDao) {
    fun getAllCamsStream(): Flow<List<Cam>> = camDao.getAllCams()

    fun getCamStream(id: Int): Flow<Cam?> = camDao.getCam(id)

    fun getActiveCamsStream(): Flow<List<Cam>> = camDao.getActive()

    suspend fun insertCam(cam: Cam):Long = camDao.insert(cam)

    suspend fun updateCam(cam: Cam): Int = camDao.update(cam)

    suspend fun deleteCam(cam: Cam) = camDao.delete(cam)

    suspend fun removeActive(id: Int): Boolean {
        return camDao.removeActive(id) > 0
    }

    suspend fun addActive(id: Int): Boolean {
        return camDao.addActive(id) > 0
    }

    suspend fun countActiveCams() : Int = camDao.countActiveCams()
}