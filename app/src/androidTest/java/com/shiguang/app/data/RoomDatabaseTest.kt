package com.shiguang.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.entity.CountdownEntity
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.data.entity.HabitRecordEntity
import com.shiguang.app.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 数据层集成测试（androidTest，真机/模拟器运行）。
 * 覆盖：倒数日增改/置顶唯一、习惯打卡与级联删除、日程（单次+周期）持久化。
 */
@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `倒数日增改与置顶唯一`() = runBlocking {
        val dao = db.countdownDao()
        val today = DateUtils.today()
        val id1 = dao.insert(
            CountdownEntity(title = "四级考试", targetEpochDay = today.plusDays(30).toEpochDay())
        )
        val id2 = dao.insert(
            CountdownEntity(
                title = "生日",
                targetEpochDay = today.plusDays(10).toEpochDay(),
                note = "送礼物",
            )
        )
        val id3 = dao.insert(
            CountdownEntity(title = "已过目标", targetEpochDay = today.minusDays(5).toEpochDay())
        )

        // 无置顶时取目标日最近的
        assertEquals(id2, dao.widgetCandidate()!!.id)

        // 置顶 id2
        dao.update(dao.getById(id2)!!.copy(pinned = true))
        assertEquals(id2, dao.widgetCandidate()!!.id)

        // 换置顶 id3：先清空再置顶（与 Repository.setPinned 语义一致）
        dao.clearPinned()
        dao.update(dao.getById(id3)!!.copy(pinned = true))
        assertEquals(id3, dao.widgetCandidate()!!.id)
        assertFalse(dao.getById(id2)!!.pinned)

        // 编辑保留备注
        dao.update(dao.getById(id1)!!.copy(title = "四级（改期）"))
        assertEquals("四级（改期）", dao.getById(id1)!!.title)
        assertEquals("送礼物", dao.getById(id2)!!.note)
    }

    @Test
    fun `习惯打卡重复忽略撤销与级联删除`() = runBlocking {
        val habitDao = db.habitDao()
        val today = DateUtils.today().toEpochDay()
        val yesterday = DateUtils.today().minusDays(1).toEpochDay()

        val id = habitDao.insert(
            HabitEntity(
                name = "背单词",
                emoji = "📚",
                intervalDays = 1,
                startEpochDay = today,
            )
        )
        habitDao.insertRecord(HabitRecordEntity(id, today))
        habitDao.insertRecord(HabitRecordEntity(id, yesterday))
        assertEquals(2, habitDao.observeAllRecords().first().size)

        // 同一周期日重复打卡被忽略
        habitDao.insertRecord(HabitRecordEntity(id, today))
        assertEquals(2, habitDao.observeAllRecords().first().size)

        // 撤销一条
        habitDao.deleteRecord(id, yesterday)
        assertEquals(1, habitDao.observeAllRecords().first().size)

        // 删除习惯 -> 记录级联删除
        val habit = habitDao.observeHabits().first().first()
        habitDao.delete(habit)
        assertTrue(habitDao.observeHabits().first().isEmpty())
        assertTrue(habitDao.observeAllRecords().first().isEmpty())
    }

    @Test
    fun `日程周期规则与单次日程持久化`() = runBlocking {
        val dao = db.scheduleDao()

        // 批量周期日程（日期范围 + 星期掩码 + 时间段）
        val today = DateUtils.today()
        val recurringId = dao.insert(
            ScheduleEntity(
                title = "英语早读",
                colorIndex = 2,
                repeatStartEpochDay = today.toEpochDay(),
                repeatEndEpochDay = today.plusDays(90).toEpochDay(),
                weekdaysMask = 1 or (1 shl 2), // 周一 + 周三
                startMinute = 8 * 60,
                endMinute = 9 * 60 + 50,
            )
        )
        val loaded = dao.observeAll().first().first()
        assertEquals(recurringId, loaded.id)
        assertEquals("英语早读", loaded.title)
        assertTrue(loaded.isRecurring)
        assertEquals(1 or (1 shl 2), loaded.weekdaysMask)
        assertEquals(8 * 60, loaded.startMinute)
        assertEquals(9 * 60 + 50, loaded.endMinute)

        // 单次日程
        dao.insert(
            ScheduleEntity(
                title = "项目组会",
                singleEpochDay = today.plusDays(2).toEpochDay(),
                startMinute = 9 * 60,
                endMinute = 10 * 60,
            )
        )
        val singles = dao.observeAll().first().filter { !it.isRecurring }
        assertEquals(1, singles.size)
        assertFalse(singles.first().isRecurring)
        assertEquals("项目组会", singles.first().title)
    }
}