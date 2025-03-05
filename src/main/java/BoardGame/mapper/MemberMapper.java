package BoardGame.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import BoardGame.constant.UserStatus;
import BoardGame.domain.Member;

@Mapper
public interface MemberMapper {
	Member selectById(Long id);
	Member selectByMemberId(String memberId);
	int insertMember(Member member);
	int updateMember(Member member);
	Member findMemberByPhone(String phone);
	Member findMemberByIdAndPhone(String memberId, String phone);
	void updatePassword(Long id, String password);
	void updateStatus(String memberId, UserStatus status);
	List<Member> selectAllOnlineUsers();
	List<Member> selectMatchingUsers();
	List<Member> selectTop5ByRatingPoint();
	List<Member> selectTop5ByWinRate();
}
