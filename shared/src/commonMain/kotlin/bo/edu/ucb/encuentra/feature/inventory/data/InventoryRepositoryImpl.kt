package bo.edu.ucb.encuentra.feature.inventory.data

import bo.edu.ucb.encuentra.core.data.local.dao.CustodyEventDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toModel
import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.inventory.domain.model.CustodyEntry
import bo.edu.ucb.encuentra.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl(
    private val foundItemDao: FoundItemDao,
    private val custodyEventDao: CustodyEventDao,
    private val userDao: UserDao
) : InventoryRepository {

    override fun observeAll(): Flow<List<FoundItem>> = foundItemDao.observeAll().map { items -> items.map { it.toModel() } }

    override fun observeItem(id: ItemId): Flow<FoundItem?> = foundItemDao.observeById(id.value).map { it?.toModel() }

    override fun observeHistory(id: ItemId): Flow<List<CustodyEntry>> =
        custodyEventDao.observeByFoundItem(id.value).map { events ->
            events.map { CustodyEntry(it.toModel(), userDao.getById(it.changedBy)?.fullName) }
        }

    override suspend fun userName(userId: String): String? = userDao.getById(userId)?.fullName
}
