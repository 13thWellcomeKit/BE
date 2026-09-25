package com.likelion13th.Welcomekit_BE.domain.dto;

import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;

// 집계 JPQL 결과 한 줄: (세션 id 또는 유저 id, 상태, 건수)
public record AttendanceStatusCount(Long groupId, AttendanceStatus status, Long count) {
}
