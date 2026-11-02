package bo.edu.ucb.encuentra.feature.reportwizard.data

import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.mapper.toEntity
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.feature.reportwizard.domain.repository.LostReportRepository

class LostReportRepositoryImpl(private val dao: LostReportDao) : LostReportRepository {
    override suspend fun save(report: LostReport) = dao.upsert(report.toEntity())
}
