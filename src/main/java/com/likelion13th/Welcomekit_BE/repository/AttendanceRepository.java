package com.likelion13th.Welcomekit_BE.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.likelion13th.Welcomekit_BE.domain.Attendance;
import com.likelion13th.Welcomekit_BE.domain.AttendanceSession;
import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
	Optional<Attendance> findByUserAndAttendanceSession(User user, AttendanceSession attendanceSession);

	List<Attendance> findAllByUserOrderByAttendanceTime(User user);

	// 인자 순서는 GetTodayAttendanceResponse 필드 순서와 같아야 한다.
	// 팀 미배정 부원도 운영진이 정정할 수 있도록 team은 LEFT JOIN.
	@Query("SELECT new com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse(" +
		"a.id, u.studentNum, t.teamName, u.userName, a.status, a.attendanceTime) " +
		"FROM attendance a " +
		"JOIN a.user u " +
		"LEFT JOIN u.team t " +
		"WHERE a.attendanceSession.sessionDate > :date " +
		"ORDER BY t.id, u.userName")
	List<GetTodayAttendanceResponse> findTodayAttendance(@Param("date") LocalDateTime date);

	@Query("SELECT new com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse(" +
		"a.id, u.studentNum, t.teamName, u.userName, a.status, a.attendanceTime) " +
		"FROM attendance a " +
		"JOIN a.user u " +
		"LEFT JOIN u.team t " +
		"WHERE a.attendanceSession.id = :sessionId " +
		"ORDER BY t.id, u.userName")
	List<GetTodayAttendanceResponse> findSessionAttendance(@Param("sessionId") Long sessionId);

	@Query("SELECT new com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount(" +
		"a.attendanceSession.id, a.status, COUNT(a)) " +
		"FROM attendance a " +
		"GROUP BY a.attendanceSession.id, a.status")
	List<AttendanceStatusCount> countBySessionAndStatus();

	@Query("SELECT new com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount(" +
		"a.user.id, a.status, COUNT(a)) " +
		"FROM attendance a " +
		"GROUP BY a.user.id, a.status")
	List<AttendanceStatusCount> countByUserAndStatus();
}
