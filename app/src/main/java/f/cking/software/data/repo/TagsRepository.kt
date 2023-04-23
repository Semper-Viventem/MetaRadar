package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagsRepository(
    database: AppDatabase,
) {

    private val dao = database.tagDao()

    suspend fun getAll(): List<String> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.name }
        }
    }

    suspend fun addNewTag(tag: String) {
        withContext(Dispatchers.IO) {
            dao.insert(TagEntity(tag))
        }
    }
}