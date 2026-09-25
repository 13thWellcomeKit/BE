package com.likelion13th.Welcomekit_BE.domain.dto.response;

import com.likelion13th.Welcomekit_BE.domain.enums.DevPart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberAttendanceSummaryResponse {
	private Long userId;
	private String userName;
	private String teamName;
	private DevPart devPart;
	private long present;
	private long late;
	private long absent;
	private Double rate; // 기록된 세션이 하나도 없으면 null
}
