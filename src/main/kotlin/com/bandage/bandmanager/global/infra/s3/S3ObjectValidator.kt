package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.properties.AwsProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

/**
 * presigned URL 로 업로드된 객체가 S3 에 실제로 존재하는지 확인한다.
 *
 * 서버는 objectKey 만 발급하고 업로드는 클라이언트가 직접 수행하므로,
 * 업로드 실패 후 등록 API 만 호출하면 깨진 이미지 참조가 저장된다(BD-275).
 */
@Component
class S3ObjectValidator(
    private val s3Client: S3Client,
    private val awsProperties: AwsProperties,
) {
    /**
     * 객체가 없으면 [ErrorCode.IMAGE_NOT_UPLOADED](400) 로 실패시킨다.
     *
     * 확인 자체가 불가능한 경우(S3 장애·권한 오류 등)는 [ErrorCode.IMAGE_STORAGE_UNAVAILABLE](502) 로 실패시킨다.
     * 저장소가 죽은 상태에서 등록을 허용해도 그 이미지는 어차피 조회되지 않으므로,
     * 깨진 참조를 남기는 대신 즉시 실패시키고 클라이언트가 재시도하게 한다.
     * 400 과 502 를 나눠 클라이언트 입력 문제와 시스템 외부 장애를 구분할 수 있게 한다.
     */
    fun requireExists(objectKey: String) {
        val request =
            HeadObjectRequest
                .builder()
                .bucket(awsProperties.s3.bucket)
                .key(objectKey)
                .build()
        try {
            s3Client.headObject(request)
        } catch (e: NoSuchKeyException) {
            log.warn("업로드되지 않은 objectKey 로 등록 시도: {}", objectKey, e)
            throw BusinessException(ErrorCode.IMAGE_NOT_UPLOADED)
        } catch (e: S3Exception) {
            // 404 는 NoSuchKeyException 으로 오지 않는 경우가 있다(HeadObject 는 본문이 없어 코드 매핑이 누락될 수 있음)
            if (e.statusCode() == 404) {
                log.warn("업로드되지 않은 objectKey 로 등록 시도: {}", objectKey, e)
                throw BusinessException(ErrorCode.IMAGE_NOT_UPLOADED)
            }
            // 403(IAM 권한 누락) 은 여기로 온다. 배포 시 s3:GetObject 권한 확인 필요.
            log.error("S3 객체 존재 확인 실패. key={}, status={}", objectKey, e.statusCode(), e)
            throw BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE)
        } catch (e: SdkException) {
            log.error("S3 객체 존재 확인 실패. key={}", objectKey, e)
            throw BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(S3ObjectValidator::class.java)
    }
}
