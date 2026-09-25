package com.likelion13th.Welcomekit_BE.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.request.CreateUserRequest;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetAllBabyLionResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetMyInfoResponse;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;
import com.likelion13th.Welcomekit_BE.exception.CustomException;
import com.likelion13th.Welcomekit_BE.exception.ErrorCode;
import com.likelion13th.Welcomekit_BE.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	@Autowired
	private final UserRepository userRepository;

	@Autowired
	private final PasswordEncoder passwordEncoder;

	public void createUser(CreateUserRequest createUserRequest) {
		User user = new User();
		user.setUserName(createUserRequest.getName());
		user.setUserType(createUserRequest.getUserType());
		user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
		user.setStudentNum(createUserRequest.getStudentNum());
		user.setDevPart(createUserRequest.getDevPart());
		user.setProfileImage("");
		userRepository.save(user);
	}

	public User getUserByStudentNum(String studentNum) {
		return userRepository.findUserByStudentNum(studentNum)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	public List<GetAllBabyLionResponse> getTotalBabyLion(User admin) {
		if (admin.getUserType() != UserType.ADMIN) {
			log.error("관리자만 조회할수있습니다.");
			throw new CustomException(ErrorCode.PERMISSION_ERROR);
		}
		log.info("아기사자 조회");
		return userRepository.findAll().stream().filter(user -> user.getUserType() == UserType.BABY_LION).map(user -> {
			GetAllBabyLionResponse babyLionResponse = new GetAllBabyLionResponse();
			babyLionResponse.setId(user.getId());
			babyLionResponse.setName(user.getUserName());
			babyLionResponse.setStudentNum(user.getStudentNum());
			babyLionResponse.setTeamName(user.getTeam() != null ? user.getTeam().getTeamName() : null);
			return babyLionResponse;
		}).toList();
	}

	public List<String> getAllNameExceptMe(String studentNum) {
		return userRepository.findAll()
			.stream()
			.filter(user -> !user.getStudentNum().equals(studentNum))
			.map(User::getUserName)
			.toList();
	}

	public List<User> getTotalBabyLionUser() {
		return userRepository.findAll().stream().filter(user -> user.getUserType() == UserType.BABY_LION).toList();
	}

	public List<GetAllBabyLionResponse> getTotalAdmin(User admin) {
		if (admin.getUserType() != UserType.ADMIN) {
			log.error("관리자만 조회할수있습니다.");
			throw new CustomException(ErrorCode.PERMISSION_ERROR);
		}
		log.debug("운영진 조회");
		return userRepository.findAll().stream().filter(user -> user.getUserType() == UserType.ADMIN).map(user -> {
			GetAllBabyLionResponse babyLionResponse = new GetAllBabyLionResponse();
			babyLionResponse.setId(user.getId());
			babyLionResponse.setName(user.getUserName());
			babyLionResponse.setStudentNum(user.getStudentNum());
			babyLionResponse.setTeamName(user.getTeam() != null ? user.getTeam().getTeamName() : null);
			return babyLionResponse;
		}).toList();
	}

	public void save(User user) {
		userRepository.save(user);
	}

	public GetMyInfoResponse getMyInfo(User user) {
		GetMyInfoResponse getMyInfoResponse = new GetMyInfoResponse();
		getMyInfoResponse.setDevPart(user.getDevPart());
		getMyInfoResponse.setProfileImage(user.getProfileImage());
		getMyInfoResponse.setStudentName(user.getStudentNum());
		getMyInfoResponse.setName(user.getUserName());
		getMyInfoResponse.setTeamName(user.getTeam() != null ? user.getTeam().getTeamName() : null);
		getMyInfoResponse.setUserType(user.getUserType());
		return getMyInfoResponse;
	}

	public void changePassword(User user, String currentPassword, String newPassword) {
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			log.error("비밀번호가 틀렸습니다.");
			throw new CustomException(ErrorCode.PASSWORD_NOT_MATCHES);
		}
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	public void saveProfileImage(MultipartFile file, User user) {
		String studentNum = user.getStudentNum();
		Path uploadDir = Paths.get("/app/external-profile/" + studentNum);

		try {
			if (!Files.exists(uploadDir)) {
				Files.createDirectories(uploadDir);
			}

			// 확장자 고정 (예: JPG)
			String fileName = "profile.jpg"; // 또는 "profile.webp"
			Path filePath = uploadDir.resolve(fileName);

			// 파일 이미 존재하면 삭제 (덮어쓰기 목적)
			if (Files.exists(filePath)) {
				Files.delete(filePath);
			}

			// BufferedImage로 읽기
			BufferedImage originalImage = ImageIO.read(file.getInputStream());
			if (originalImage == null) {
				log.error("Image가 null입니다.");
				throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT);
			}

			// 이미지 리사이즈 (최대 300px)
			int width = originalImage.getWidth();
			int height = originalImage.getHeight();
			int maxSize = 300;
			float scale = Math.min((float)maxSize / width, (float)maxSize / height);
			int newWidth = Math.round(width * scale);
			int newHeight = Math.round(height * scale);

			Image resized = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
			BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = resizedImage.createGraphics();
			g.drawImage(resized, 0, 0, null);
			g.dispose();

			// 압축해서 저장 (JPEG 예시)
			try (OutputStream os = Files.newOutputStream(filePath)) {
				ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
				ImageOutputStream ios = ImageIO.createImageOutputStream(os);
				writer.setOutput(ios);

				ImageWriteParam param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.7f); // 70% 퀄리티로 압축

				writer.write(null, new IIOImage(resizedImage, null, null), param);
				writer.dispose();
				ios.close();
			}

			// DB 경로 저장
			String filePath2 = "/app/external-profile/" + studentNum + "/" + fileName;
			user.setProfileImage(filePath2);

			userRepository.save(user);

		} catch (IOException e) {
			log.error("프로필 이미지 업로드 실패", e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public Resource getProfileImage(User user) {
		if (user.getProfileImage().isEmpty()) {
			log.error("아직 프로필 이미지가 없습니다. 기본 이미지를 제공해드리겠습니다.");
			throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
		}
		String imagePath = user.getProfileImage(); // 예: "profile/201802265/profile.jpg"
		Path path = Paths.get(imagePath);

		try {
			Resource resource = new UrlResource(path.toUri());
			if (!resource.exists()) {
				log.error("이미지를 찾을수없습니다.");
				throw new CustomException(ErrorCode.IMAGE_NOT_FOUND);
			}
			return resource;

		} catch (IOException e) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	public String getProfileImageContentType(Resource resource) {
		try {
			return Files.probeContentType(resource.getFile().toPath());
		} catch (IOException e) {
			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
	}
}
