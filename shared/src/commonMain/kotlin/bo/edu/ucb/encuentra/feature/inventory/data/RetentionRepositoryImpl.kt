package bo.edu.ucb.encuentra.feature.inventory.data

import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.RetentionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RetentionRepositoryImpl(
    private val foundItemDao: FoundItemDao,
    private val userDao: UserDao
) : RetentionRepository {

    override fun observeAvailable(): Flow<List<FoundItem>> =
        foundItemDao.observeByStatus(ItemStatus.DISPONIBLE).map { list -> list.map { it.toModel() } }

    override suspend fun availableItems(): List<FoundItem> =
        foundItemDao.getByStatus(ItemStatus.DISPONIBLE).map { it.toModel() }

    override suspend fun findItem(id: String): FoundItem? = foundItemDao.getById(id)?.toModel()

    override suspend fun staffIds(): List<String> = userDao.getStaff().map { it.id }
}
