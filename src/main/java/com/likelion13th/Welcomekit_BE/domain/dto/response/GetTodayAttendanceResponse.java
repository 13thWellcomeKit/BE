package com.likelion13th.Welcomekit_BE.domain.dto.response;

import java.time.LocalDateTime;

import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AttendanceRepository의 JPQL 생성자 표현식이 이 필드 순서를 그대로 따른다. 순서를 바꾸면 쿼리도 같이 바꿀 것.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetTodayAttendanceResponse {
	private Long attendanceId;
	private String studentNum;
	private String teamName;
	private String name;
	private AttendanceStatus attendanceStatus;
	private LocalDateTime attendanceTime;
}
