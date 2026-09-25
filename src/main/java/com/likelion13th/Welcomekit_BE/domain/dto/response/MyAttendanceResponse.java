package com.likelion13th.Welcomekit_BE.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MyAttendanceResponse {
	private LocalDate date;
	private AttendanceStatus attendanceStatus;
	private LocalDateTime attendanceTime;
}