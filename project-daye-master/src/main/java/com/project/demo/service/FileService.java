package com.project.demo.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {
    
    // src/main/resources/static/ 내부로 변경
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/videos/";
    private static final String THUMBNAIL_DIR = "src/main/resources/static/uploads/thumbnails/";
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    private static final String[] ALLOWED_EXTENSIONS = {".mp4", ".webm", ".ogv", ".avi", ".mov", ".mkv"};
    
    public FileService() {
        createDirectories();
    }
    
    private void createDirectories() {
        try {
            Path videoDir = Paths.get(UPLOAD_DIR);
            Path thumbnailDir = Paths.get(THUMBNAIL_DIR);
            
            Files.createDirectories(videoDir);
            Files.createDirectories(thumbnailDir);
            
            System.out.println("✅ 디렉토리 생성 완료:");
            System.out.println("   비디오: " + videoDir.toAbsolutePath());
            System.out.println("   썸네일: " + thumbnailDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ 디렉토리 생성 실패: " + e.getMessage());
            throw new RuntimeException("디렉토리 생성에 실패했습니다.", e);
        }
    }
    
    public String uploadVideo(MultipartFile file) {
        System.out.println("=== 동영상 업로드 시작 ===");
        System.out.println("원본 파일명: " + file.getOriginalFilename());
        System.out.println("파일 크기: " + file.getSize() + " bytes");
        System.out.println("Content Type: " + file.getContentType());
        
        validateVideoFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + extension;
        
        System.out.println("생성된 파일명: " + filename);
        
        try {
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.copy(file.getInputStream(), filePath);
            
            File uploadedFile = filePath.toFile();
            System.out.println("업로드 완료: " + uploadedFile.exists() + ", 크기: " + uploadedFile.length() + " bytes");
            
            return filename;
        } catch (IOException e) {
            System.err.println("❌ 파일 업로드 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }
    
    public String generateThumbnail(String videoFilename) {
        System.out.println("=== JavaCV 썸네일 생성 시작: " + videoFilename + " ===");
        
        String thumbnailFilename = UUID.randomUUID().toString() + ".jpg";
        String videoPath = UPLOAD_DIR + videoFilename;
        String thumbnailPath = THUMBNAIL_DIR + thumbnailFilename;
        
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            System.err.println("❌ 비디오 파일이 존재하지 않습니다: " + videoPath);
            return createDefaultThumbnail(thumbnailFilename);
        }
        
        System.out.println("비디오 파일: " + videoFile.getAbsolutePath() + " (크기: " + videoFile.length() + " bytes)");
        
        FFmpegFrameGrabber grabber = null;
        try {
            // FFmpegFrameGrabber 설정
            grabber = new FFmpegFrameGrabber(videoPath);
            
            System.out.println("🎬 동영상 정보 분석 중...");
            grabber.start();
            
            // 동영상 정보 출력
            int videoLength = grabber.getLengthInFrames();
            double frameRate = grabber.getFrameRate();
            int videoWidth = grabber.getImageWidth();
            int videoHeight = grabber.getImageHeight();
            double duration = grabber.getLengthInTime() / 1000000.0; // 마이크로초를 초로 변환
            
            System.out.println("   해상도: " + videoWidth + "x" + videoHeight);
            System.out.println("   프레임 수: " + videoLength);
            System.out.println("   프레임 레이트: " + frameRate);
            System.out.println("   재생 시간: " + String.format("%.2f", duration) + "초");
            
            if (videoLength <= 0 || videoWidth <= 0 || videoHeight <= 0) {
                System.err.println("❌ 동영상 정보가 올바르지 않습니다.");
                return createDefaultThumbnail(thumbnailFilename);
            }
            
            // 썸네일 추출 시점 계산 (여러 지점 시도)
            double[] timePoints = {
                Math.min(1.0, duration * 0.1),    // 10% 지점 또는 1초
                Math.min(2.0, duration * 0.2),    // 20% 지점 또는 2초
                Math.min(5.0, duration * 0.3),    // 30% 지점 또는 5초
                duration * 0.5,                   // 50% 지점 (중간)
                0.5                               // 0.5초 (마지막 시도)
            };
            
            for (double timePoint : timePoints) {
                if (timePoint >= duration) continue;
                
                System.out.println("⏰ " + String.format("%.1f", timePoint) + "초 지점에서 프레임 추출 시도");
                
                // 해당 시점으로 이동
                long timestampMicros = (long)(timePoint * 1000000);
                grabber.setTimestamp(timestampMicros);
                
                // 프레임 추출
                Frame frame = grabber.grabImage();
                if (frame != null && frame.image != null) {
                    
                    // Frame을 BufferedImage로 변환
                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    BufferedImage bufferedImage = converter.convert(frame);
                    
                                         if (bufferedImage != null) {
                         // 썸네일 크기로 리사이즈 (16:9 비율에 맞춤)
                         BufferedImage thumbnail = resizeImage(bufferedImage, 800, 450);
                        
                        // 파일로 저장
                        File thumbnailFile = new File(thumbnailPath);
                        boolean saved = ImageIO.write(thumbnail, "jpg", thumbnailFile);
                        
                        if (saved && thumbnailFile.exists() && thumbnailFile.length() > 1000) {
                            System.out.println("✅ JavaCV 썸네일 생성 성공: " + thumbnailFilename + 
                                             " (시점: " + String.format("%.1f", timePoint) + "초, 크기: " + 
                                             thumbnailFile.length() + " bytes)");
                            return thumbnailFilename;
                        }
                    }
                }
            }
            
            System.err.println("❌ 모든 시점에서 썸네일 추출 실패");
            return createDefaultThumbnail(thumbnailFilename);
            
        } catch (Exception e) {
            System.err.println("❌ JavaCV 썸네일 생성 중 오류: " + e.getMessage());
            e.printStackTrace();
            return createDefaultThumbnail(thumbnailFilename);
        } finally {
            // 리소스 정리
            if (grabber != null) {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (Exception e) {
                    System.err.println("⚠️ Grabber 리소스 해제 중 오류: " + e.getMessage());
                }
            }
        }
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 비율 유지하면서 크기 계산
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;
        
        if (aspectRatio > (double) targetWidth / targetHeight) {
            // 가로가 더 긴 경우
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        } else {
            // 세로가 더 긴 경우
            newWidth = (int) (targetHeight * aspectRatio);
            newHeight = targetHeight;
        }
        
        // 고품질 리사이징
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // 렌더링 품질 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 검은색 배경
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        
        // 중앙에 이미지 그리기
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;
        g2d.drawImage(originalImage, x, y, newWidth, newHeight, null);
        
        g2d.dispose();
        
        return resizedImage;
    }
    
    private String createDefaultThumbnail(String thumbnailFilename) {
        try {
            System.out.println("🎨 기본 썸네일 생성: " + thumbnailFilename);
            
            BufferedImage defaultThumbnail = new BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = defaultThumbnail.createGraphics();
            
            // 렌더링 품질 설정
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
                         // 그라데이션 배경
             GradientPaint gradient = new GradientPaint(
                 0, 0, new Color(45, 45, 45),
                 800, 450, new Color(25, 25, 25)
             );
                         g2d.setPaint(gradient);
             g2d.fillRect(0, 0, 800, 450);
             
             // 테두리
             g2d.setColor(new Color(70, 70, 70));
             g2d.setStroke(new BasicStroke(2));
             g2d.drawRect(5, 5, 790, 440);
            
                         // 재생 버튼 (원형 배경)
             g2d.setColor(new Color(255, 255, 255, 180));
             g2d.fillOval(375, 200, 50, 50);
             
             // 재생 버튼 (삼각형)
             g2d.setColor(new Color(60, 60, 60));
             int[] xPoints = {390, 390, 415};
             int[] yPoints = {210, 240, 225};
             g2d.fillPolygon(xPoints, yPoints, 3);
            
                         // 텍스트
             g2d.setColor(new Color(200, 200, 200));
             g2d.setFont(new Font("Arial", Font.BOLD, 16));
             FontMetrics fm = g2d.getFontMetrics();
             String text = "VIDEO";
             int textWidth = fm.stringWidth(text);
             g2d.drawString(text, (800 - textWidth) / 2, 370);
             
             g2d.setFont(new Font("Arial", Font.PLAIN, 11));
             fm = g2d.getFontMetrics();
             String subText = "썸네일을 생성할 수 없습니다";
             int subTextWidth = fm.stringWidth(subText);
             g2d.drawString(subText, (800 - subTextWidth) / 2, 390);
            
            g2d.dispose();
            
            // 파일 저장
            File outputFile = new File(THUMBNAIL_DIR + thumbnailFilename);
            boolean success = ImageIO.write(defaultThumbnail, "jpg", outputFile);
            
            if (success && outputFile.exists()) {
                System.out.println("✅ 기본 썸네일 생성 완료: " + outputFile.length() + " bytes");
                return thumbnailFilename;
            } else {
                throw new RuntimeException("기본 썸네일 파일 저장 실패");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 기본 썸네일 생성 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("기본 썸네일 생성에 실패했습니다.", e);
        }
    }
    
    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("파일이 비어있습니다.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("파일 크기가 2GB를 초과합니다.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("파일명이 없습니다.");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean isValidExtension = false;
        
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (allowedExtension.equals(extension)) {
                isValidExtension = true;
                break;
            }
        }
        
        if (!isValidExtension) {
            throw new RuntimeException("지원하지 않는 파일 형식입니다. (mp4, webm, ogv, avi, mov, mkv만 허용)");
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
    
    public void deleteFile(String filename) {
        if (filename != null && !filename.isEmpty()) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                boolean deleted = Files.deleteIfExists(filePath);
                System.out.println(deleted ? "✅ 비디오 파일 삭제: " + filename : "ℹ️ 비디오 파일이 없음: " + filename);
            } catch (IOException e) {
                System.err.println("❌ 파일 삭제 실패: " + filename + " - " + e.getMessage());
            }
        }
    }
    
    public void deleteThumbnail(String thumbnailFilename) {
        if (thumbnailFilename != null && !thumbnailFilename.isEmpty()) {
            try {
                Path thumbnailPath = Paths.get(THUMBNAIL_DIR + thumbnailFilename);
                boolean deleted = Files.deleteIfExists(thumbnailPath);
                System.out.println(deleted ? "✅ 썸네일 파일 삭제: " + thumbnailFilename : "ℹ️ 썸네일 파일이 없음: " + thumbnailFilename);
            } catch (IOException e) {
                System.err.println("❌ 썸네일 삭제 실패: " + thumbnailFilename + " - " + e.getMessage());
            }
        }
    }
    
    public File getVideoFile(String filename) {
        return Paths.get(UPLOAD_DIR + filename).toFile();
    }
    
    public File getThumbnailFile(String filename) {
        return Paths.get(THUMBNAIL_DIR + filename).toFile();
    }
}