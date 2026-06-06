# OpenAPI Diff 도구 선정 가이드

본 문서는 BE/FE 영향평가(스펙 diff) Tool 및 로컬 breaking-change 점검에 사용할 diff 도구의
선정 기준과 후보를 정리한다.

> 대상 스펙: `docs/openapi.json` (OAS **3.1.0**, 단일 파일, `$ref` 외부 분할 없음)

## 선정 기준 (필수)

1. **OAS 3.1.0 지원**: 본 프로젝트 스펙은 3.1.0 출력이다. 3.0 전용 도구는 3.1의 스키마 모델
   차이(예: `type` 배열/`null` 표현, `examples` 등)로 **오탐/누락 위험**이 있다. → 도입 전 반드시 검증.
2. **Breaking change 판정**: 필드 삭제, 타입 변경, required 추가, enum 값 제거 등을 자동 분류.
3. **단일 파일 입력**: 번들링 선행 단계 없이 `docs/openapi.json` 두 버전을 직접 비교 가능.
4. **CLI / CI 통합 용이**: 원격 raw URL 입력 또는 로컬 파일 비교 지원.

## 후보 (도입 시점에 3.1 지원 실측 필요)

| 도구 | 형태 | 3.1.0 지원 | 비고 |
| --- | --- | --- | --- |
| oasdiff | Go, 단일 바이너리 | **검증 필요** (3.1 지원 표방) | CLI/GitHub Action 제공, breaking 분류 내장 |
| openapi-diff (OpenAPITools) | Java | **검증 필요** (3.0 중심으로 알려짐) | 3.1 지원 범위 확인 요망 |
| optic | SaaS/CLI | **검증 필요** | 무료 티어 제한 확인 요망 |

> 위 "지원" 칸은 미확정이다. 각 도구의 현행 릴리스 문서로 3.1.0 지원을 확인한 뒤 표를 갱신할 것.
> 빠른 실측 방법: 후보 도구로 `docs/openapi.json` 을 자기 자신과 비교해 **파싱 오류 없이 "차이 없음"** 이
> 나오는지부터 확인한다(3.1 미지원이면 파싱 단계에서 실패하거나 오탐이 발생한다).

## 선정 절차

1. 후보 도구를 설치하고 `docs/openapi.json` 자기 비교로 3.1 파싱 가능 여부 확인.
2. 의도적 breaking(필드 삭제 등) / non-breaking(옵셔널 필드 추가) 샘플로 판정 정확도 확인.
3. 원격 raw URL 두 ref 비교가 동작하는지 확인.
4. 통과한 도구를 본 문서 표에 "지원 확인됨"으로 확정 기록.

## 사용 예시 (도구 확정 후 채울 자리)

```bash
# 예: 두 ref 의 커밋된 스펙 비교 (도구 확정 후 실제 명령으로 대체)
#   base: https://raw.githubusercontent.com/TeamBandage/bandage-band-manager/<base_ref>/docs/openapi.json
#   head: https://raw.githubusercontent.com/TeamBandage/bandage-band-manager/<head_ref>/docs/openapi.json
#   <diff-tool> breaking <base_url> <head_url>
```

## MCP Tool 연동 메모

MCP 영향평가 Tool 은 레포가 public 이므로 인증 없이 ref 별 스펙을 가져올 수 있다:

```
https://raw.githubusercontent.com/TeamBandage/bandage-band-manager/{ref}/docs/openapi.json
```

비인증 GitHub 접근의 레이트리밋(IP당 시간당 60회)이 문제되면 토큰으로 상향한다.
