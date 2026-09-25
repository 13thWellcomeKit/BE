package com.likelion13th.Welcomekit_BE.domain.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceSessionSummaryResponse {
	private Long sessionId;
	private LocalDateTime sessionDate;
	private long presentCount;
	private long lateCount;
	private long absentCount;
}
