package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.properties.AwsProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception

/**
 * 업로드되지 않은 objectKey 로 이미지가 등록되던 문제(BD-275) 검증.
 *
 * 서버는 objectKey 만 발급하고 업로드는 클라이언트가 수행하므로,
 * 업로드 실패 후 등록만 호출하면 깨진 참조가 저장됐다.
 */
class S3ObjectValidatorTest {
    private val s3Client = mock(S3Client::class.java)
    private val awsProperties =
        AwsProperties(
            credentials = AwsProperties.Credentials("ak", "sk"),
            region = AwsProperties.Region("ap-northeast-2"),
            s3 = AwsProperties.S3("test-bucket"),
            cloudfront = AwsProperties.CloudFront("cdn.example.com"),
        )
    private val sut = S3ObjectValidator(s3Client, awsProperties)

    @Test
    fun `객체가 존재하면 통과한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenReturn(HeadObjectResponse.builder().build())

        assertThatCode { sut.requireExists("poster/performance/1/a.jpg") }.doesNotThrowAnyException()
    }

    @Test
    fun `객체가 없으면 IMAGE_NOT_UPLOADED 로 실패한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenThrow(NoSuchKeyException.builder().message("not found").build())

        val exception = assertThrows<BusinessException> { sut.requireExists("poster/does/not/exist.png") }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.IMAGE_NOT_UPLOADED)
    }

    @Test
    fun `HeadObject 가 404 로 응답해도 IMAGE_NOT_UPLOADED 로 실패한다`() {
        // HeadObject 는 응답 본문이 없어 NoSuchKeyException 으로 매핑되지 않는 경우가 있다
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenThrow(
                S3Exception
                    .builder()
                    .statusCode(404)
                    .message("Not Found")
                    .build(),
            )

        val exception = assertThrows<BusinessException> { sut.requireExists("poster/does/not/exist.png") }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.IMAGE_NOT_UPLOADED)
    }

    @Test
    fun `S3 장애로 확인이 불가하면 IMAGE_STORAGE_UNAVAILABLE 로 실패한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenThrow(SdkClientException.create("connection reset"))

        val exception = assertThrows<BusinessException> { sut.requireExists("poster/performance/1/a.jpg") }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.IMAGE_STORAGE_UNAVAILABLE)
    }

    @Test
    fun `IAM 권한 누락 등 403 은 IMAGE_STORAGE_UNAVAILABLE 로 실패한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenThrow(
                S3Exception
                    .builder()
                    .statusCode(403)
                    .message("Forbidden")
                    .build(),
            )

        val exception = assertThrows<BusinessException> { sut.requireExists("poster/performance/1/a.jpg") }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.IMAGE_STORAGE_UNAVAILABLE)
    }

    @Test
    fun `입력 문제(400)와 저장소 장애(502)는 상태 코드로 구분된다`() {
        assertThat(ErrorCode.IMAGE_NOT_UPLOADED.status.value()).isEqualTo(400)
        assertThat(ErrorCode.IMAGE_STORAGE_UNAVAILABLE.status.value()).isEqualTo(502)
    }

    @Test
    fun `설정된 버킷과 전달받은 키로 조회한다`() {
        `when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
            .thenReturn(HeadObjectResponse.builder().build())
        val captor = ArgumentCaptor.forClass(HeadObjectRequest::class.java)

        sut.requireExists("profile/band/1/a.jpg")

        verify(s3Client).headObject(captor.capture())
        assertThat(captor.value.bucket()).isEqualTo("test-bucket")
        assertThat(captor.value.key()).isEqualTo("profile/band/1/a.jpg")
    }
}
