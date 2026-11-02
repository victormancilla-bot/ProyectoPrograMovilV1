package bo.edu.ucb.encuentra.core.data

import bo.edu.ucb.encuentra.core.data.local.dao.CustodyEventDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.CustodyEvent
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.repository.FoundItemLifecycleRepository
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoundItemLifecycleRepositoryImpl(
    private val foundItemDao: FoundItemDao,
    private val custodyEventDao: CustodyEventDao
) : FoundItemLifecycleRepository {

    override suspend fun getById(id: ItemId): FoundItem? = foundItemDao.getById(id.value)?.toModel()

    override suspend fun updateStatus(id: ItemId, status: ItemStatus) = foundItemDao.updateStatus(id.value, status)

    override suspend fun moveCustody(event: CustodyEvent) {
        custodyEventDao.insert(event.toEntity())
        foundItemDao.updateCustody(event.foundItemId.value, event.place, event.detail)
    }

    override fun observeCustodyHistory(id: ItemId): Flow<List<CustodyEvent>> =
        custodyEventDao.observeByFoundItem(id.value).map { events -> events.map { it.toModel() } }
}
