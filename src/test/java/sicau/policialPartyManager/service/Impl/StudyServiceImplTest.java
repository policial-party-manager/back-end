package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sicau.policialPartyManager.api.dto.StudyRecordUpsertRequest;
import sicau.policialPartyManager.api.dto.StudySaveRequest;
import sicau.policialPartyManager.api.dto.StudyRecordVo;
import sicau.policialPartyManager.api.dto.StudySummaryVo;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.Study;
import sicau.policialPartyManager.model.entity.StudyRecord;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.StudyMapper;
import sicau.policialPartyManager.repository.StudyRecordMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学习域单测（#32 Agent Brief 验收对照）：课程字典 + 记录 upsert/权限面/汇总。
 */
@ExtendWith(MockitoExtension.class)
class StudyServiceImplTest {

    @Mock
    private StudyRecordMapper recordMapper;
    @Mock
    private StudyMapper studyMapper;
    @Mock
    private UserDetailMapper userDetailMapper;
    @Mock
    private BranchMapper branchMapper;

    private StudyServiceImpl studyService;
    private StudyRecordServiceImpl recordService;

    @BeforeAll
    static void initMybatisPlusCache() {
        // 裸单测无 Spring 上下文：注册实体 TableInfo，否则 getSqlSegment 物化时
        // 抛 "can not find lambda cache"（#33 同款坑，经验已入仓）
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StudyRecord.class);
        TableInfoHelper.initTableInfo(assistant, Study.class);
        TableInfoHelper.initTableInfo(assistant, UserDetail.class);
        TableInfoHelper.initTableInfo(assistant, Branch.class);
    }

    private Study course;
    private User superAdmin;
    private User secretary;
    private User plainMember;

    @BeforeEach
    void setUp() {
        studyService = new StudyServiceImpl(studyMapper, recordMapper);
        recordService = new StudyRecordServiceImpl(recordMapper, studyMapper, userDetailMapper, branchMapper);

        course = new Study();
        course.setId(3L);
        course.setName("党章党风专题");
        course.setType("专题");

        superAdmin = new User("superadmin", "super_admin", 1L);
        secretary = new User("sec001", "branch_admin", 7L);
        plainMember = new User("stu001", "student", 42L);
    }

    // ---------- 课程字典 ----------

    @Test
    void courseCreateRejectsDuplicateName() {
        StudySaveRequest request = courseRequest("党章党风专题", "专题");
        when(studyMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> studyService.create(request));
        assertTrue(e.getMessage().contains("已存在"), e.getMessage());
        verify(studyMapper, never()).insert(any(Study.class));
    }

    @Test
    void courseCreateTrimsAndPersists() {
        StudySaveRequest request = courseRequest(" 新生党课题 ", "必修");
        when(studyMapper.selectCount(any())).thenReturn(0L);

        Study saved = studyService.create(request);

        assertEquals("新生党课题", saved.getName());
        assertEquals("必修", saved.getType());
        verify(studyMapper).insert(saved);
    }

    @Test
    void courseDeleteBlockedWhenReferenced() {
        when(studyMapper.selectById(3L)).thenReturn(course);
        StudyRecord ref = new StudyRecord();
        ref.setCourseId(3L);
        when(recordMapper.selectCount(any())).thenReturn(2L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> studyService.delete(3L));
        assertTrue(e.getMessage().contains("不能删除"), e.getMessage());
        verify(studyMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void courseUpdateUnknownRejected() {
        when(studyMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> studyService.update(99L, courseRequest("x", "必修")));
    }

    // ---------- 记录 upsert：权限面 ----------

    @Test
    void superAdminUpsertCreatesRecordForAnyUser() {
        givenCourseAndUser(42L, "张三", 3L);
        when(recordMapper.selectOne(any())).thenReturn(null);

        StudyRecordVo vo = recordService.upsert(recordRequest(42L, 3L), superAdmin);

        assertEquals("张三", vo.getStudentName());
        assertEquals("党章党风专题", vo.getCourseName());
        verify(recordMapper).insert(any(StudyRecord.class));
    }

    @Test
    void branchAdminCanWriteForOwnBranchMember() {
        givenCourseAndUser(42L, "张三", 3L);
        givenSecretaryOf(7L, 3L);
        when(recordMapper.selectOne(any())).thenReturn(null);

        StudyRecordVo vo = recordService.upsert(recordRequest(42L, 3L), secretary);

        assertEquals("张三", vo.getStudentName());
    }

    @Test
    void branchAdminWritingOtherBranchMemberSeesUserNotFound() {
        givenCourse(3L);
        UserDetail foreign = detail(43L, "外支部", 9L); // 支部 9 非其管辖
        when(userDetailMapper.selectById(43L)).thenReturn(foreign);
        givenSecretaryOf(7L, 3L);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> recordService.upsert(recordRequest(43L, 3L), secretary));
        assertEquals("用户不存在", e.getMessage());
        verify(recordMapper, never()).insert(any(StudyRecord.class));
    }

    @Test
    void branchAdminWithoutManagedBranchRejected() {
        givenCourse(3L);
        when(branchMapper.selectList(any())).thenReturn(List.of()); // 不是任何支部的秘书

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> recordService.upsert(recordRequest(42L, 3L), secretary));
        assertTrue(e.getMessage().contains("未管理任何支部"), e.getMessage());
    }

    @Test
    void memberCannotWrite() {
        givenCourse(3L);
        // 普通成员调录入：非秘书 -> 无管辖支部，直接拒绝
        when(branchMapper.selectList(any())).thenReturn(List.of());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> recordService.upsert(recordRequest(9L, 3L), plainMember));
        assertTrue(e.getMessage().contains("未管理任何支部"), e.getMessage());
    }

    @Test
    void upsertWithUnknownCourseRejected() {
        when(studyMapper.selectById(3L)).thenReturn(null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> recordService.upsert(recordRequest(42L, 3L), superAdmin));
        assertTrue(e.getMessage().contains("课程不存在"), e.getMessage());
    }

    // ---------- upsert 覆盖语义 ----------

    @Test
    void secondUpsertUpdatesExistingRow() {
        givenCourseAndUser(42L, "张三", 3L);
        StudyRecord existing = new StudyRecord();
        existing.setId(8L);
        existing.setUserId(42L);
        existing.setCourseId(3L);
        existing.setStudyTime(LocalTime.of(1, 0));
        existing.setScore(new BigDecimal("70.00"));
        when(recordMapper.selectOne(any())).thenReturn(existing);

        StudyRecordVo vo = recordService.upsert(recordRequest(42L, 3L), superAdmin);

        assertEquals(new BigDecimal("85.50"), vo.getScore());
        assertEquals(8L, vo.getId());
        verify(recordMapper).updateById(existing);
        verify(recordMapper, never()).insert(any(StudyRecord.class));
    }

    @Test
    void scoreDecimalsSurviveRoundtrip() {
        givenCourseAndUser(42L, "张三", 3L);
        when(recordMapper.selectOne(any())).thenReturn(null);

        StudyRecordVo vo = recordService.upsert(recordRequest(42L, 3L), superAdmin);

        assertEquals(new BigDecimal("85.50"), vo.getScore()); // decimal(5,2) 精度决策
    }

    // ---------- 删除 ----------

    @Test
    void branchAdminDeletingForeignRecordSeesNotFound() {
        StudyRecord foreign = new StudyRecord();
        foreign.setId(9L);
        foreign.setUserId(43L);
        when(recordMapper.selectById(9L)).thenReturn(foreign);
        givenSecretaryOf(7L, 3L);
        when(userDetailMapper.selectById(43L)).thenReturn(detail(43L, "外支部", 9L));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> recordService.delete(9L, secretary));
        assertEquals("学习记录不存在", e.getMessage());
        verify(recordMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void superAdminDeletesAnyRecord() {
        StudyRecord mine = new StudyRecord();
        mine.setId(9L);
        mine.setUserId(42L);
        when(recordMapper.selectById(9L)).thenReturn(mine);

        recordService.delete(9L, superAdmin);

        verify(recordMapper).deleteById(9L);
    }

    // ---------- 成员可见性与汇总 ----------

    @Test
    void minePageFiltersByCurrentUserId() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<StudyRecord>();
        page.setRecords(List.of());
        page.setTotal(0L);
        when(recordMapper.selectPage(any(), any())).thenReturn(page);

        recordService.pageMine(1, 10, plainMember);

        ArgumentCaptor<AbstractWrapper<StudyRecord, ?, ?>> captor = ArgumentCaptor.forClass(AbstractWrapper.class);
        verify(recordMapper).selectPage(any(), captor.capture());
        captor.getValue().getSqlSegment(); // 强制物化 lambda 条件
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(42L),
                "成员列表必须锁定当前用户");
    }

    @Test
    void summarySumsMinutesAndAveragesScore() {
        StudyRecord a = rec(LocalTime.of(2, 30), "80.00");
        StudyRecord b = rec(LocalTime.of(1, 30), "90.50");
        when(recordMapper.selectList(any())).thenReturn(List.of(a, b));

        StudySummaryVo summary = recordService.summaryMine(plainMember);

        assertEquals(2, summary.getRecordCount());
        assertEquals(240, summary.getTotalMinutes()); // 2.5h + 1.5h
        assertEquals(new BigDecimal("85.25"), summary.getAvgScore());
    }

    @Test
    void summaryEmptyIsNullAvg() {
        when(recordMapper.selectList(any())).thenReturn(List.of());
        StudySummaryVo summary = recordService.summaryMine(plainMember);
        assertEquals(0, summary.getRecordCount());
        assertNull(summary.getAvgScore());
    }

    // ---------- helpers ----------

    private void givenCourse(Long id) {
        Study c = new Study();
        c.setId(id);
        c.setName("党章党风专题");
        c.setType("专题");
        when(studyMapper.selectById(id)).thenReturn(c);
    }

    private void givenCourseAndUser(Long userId, String name, Long branchId) {
        givenCourse(3L);
        when(userDetailMapper.selectById(userId)).thenReturn(detail(userId, name, branchId));
    }

    private void givenSecretaryOf(Long secretaryId, Long branchId) {
        Branch branch = new Branch();
        branch.setId(branchId);
        branch.setSecretaryId(secretaryId);
        branch.setStatus(1);
        when(branchMapper.selectList(any())).thenReturn(List.of(branch));
    }

    private UserDetail detail(Long userId, String name, Long branchId) {
        UserDetail d = new UserDetail();
        d.setUserId(userId);
        d.setName(name);
        d.setBranchId(branchId);
        return d;
    }

    private StudyRecord rec(LocalTime time, String score) {
        StudyRecord r = new StudyRecord();
        r.setStudyTime(time);
        r.setScore(new BigDecimal(score));
        return r;
    }

    private StudySaveRequest courseRequest(String name, String type) {
        StudySaveRequest r = new StudySaveRequest();
        r.setName(name);
        r.setType(type);
        return r;
    }

    private StudyRecordUpsertRequest recordRequest(Long userId, Long courseId) {
        StudyRecordUpsertRequest r = new StudyRecordUpsertRequest();
        r.setUserId(userId);
        r.setCourseId(courseId);
        r.setStudyTime(LocalTime.of(3, 0));
        r.setScore(new BigDecimal("85.50"));
        return r;
    }
}
