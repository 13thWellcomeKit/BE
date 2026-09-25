package com.likelion13th.Welcomekit_BE.domain.dto.request;

import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateAttendanceStatusRequest {
	private AttendanceStatus status;
}
