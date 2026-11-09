package sicau.policialPartyManager.service;

import sicau.policialPartyManager.service.Impl.MentorServiceImpl;

import java.util.List;

/**
 * 培养联系人只读查询（#68-L1）。
 * <p>抽成接口的目的很实际：思想汇报服务需要「取某人当前联系人」，
 * 但 {@code MentorServiceImpl} 是具体类，本机 JDK 25 下 Byte Buddy 无法 mock 它，
 * 导致所有引用它的单测都跑不起来。窄接口让调用方只依赖「能读」这一件事。
 */
public interface MentorQueryService {

    /** 某成员当前在职培养联系人明细（id + 姓名）。 */
    List<MentorServiceImpl.MentorView> mentorViewsOf(Long userId);
}
