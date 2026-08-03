# DB 계정 분리 적용 절차 (BD-268)

침해사고 대응으로 단일 슈퍼유저 계정(`<OLD_ADMIN>`) 접속을 3개 역할로 분리했다.
슈퍼유저가 아니면 `COPY ... FROM PROGRAM` 을 통한 원격 코드 실행이 불가능하다.

| 역할 | 환경변수 | 용도 | 권한 |
|---|---|---|---|
| `<DB_ADMIN>` | `DB_ADMIN` | 컨테이너 부트스트랩 / 수동 관리 | 슈퍼유저. 애플리케이션은 사용하지 않음 |
| `<DB_OWNER>` | `DB_OWNER` | Liquibase 마이그레이션 | 스키마 소유자 (DDL) |
| `<DB_USER>` | `DB_USER` | 애플리케이션 런타임 | `SELECT/INSERT/UPDATE/DELETE` 만 |

기존 탈취 계정(`<OLD_ADMIN>`)은 전환 확인 후 권한을 회수한다.

> `<...>` 는 플레이스홀더다. 실제 계정명은 레포에 기록하지 않으며,
> GitHub Repository Secrets(`DB_ADMIN` / `DB_OWNER` / `DB_USER`)에 등록된 값으로 대체해 실행할 것.

## 신규 환경

`scripts/db-init/01-roles.sh` 가 postgres 컨테이너 최초 기동 시 자동 실행되어 역할을 생성한다.
별도 작업 없음.

## 기존 환경 (데이터가 이미 있는 볼륨)

`/docker-entrypoint-initdb.d` 스크립트는 데이터 디렉토리가 비어 있을 때만 실행되므로,
운영 중인 DB 에는 아래를 **슈퍼유저로 1회 수동 실행**한다.
비밀번호는 셸 히스토리에 남지 않도록 `psql` 대화형 세션에서 입력할 것.

```sql
-- 1) 역할 생성 (비밀번호는 실제 값으로 대체)
CREATE ROLE <DB_OWNER> LOGIN PASSWORD :'owner_pw';
CREATE ROLE <DB_USER> LOGIN PASSWORD :'app_pw';

-- 2) 기존 객체 소유권을 owner 로 이전
ALTER SCHEMA public OWNER TO <DB_OWNER>;
REASSIGN OWNED BY <OLD_ADMIN> TO <DB_OWNER>;

-- 3) public 의 암묵적 CREATE 권한 회수
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- 4) app 에 DML 만 부여
GRANT CONNECT ON DATABASE bandage TO <DB_USER>;
GRANT USAGE ON SCHEMA public TO <DB_USER>;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO <DB_USER>;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <DB_USER>;

-- 5) owner 가 이후 생성하는 객체에도 자동 적용
ALTER DEFAULT PRIVILEGES FOR ROLE <DB_OWNER> IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO <DB_USER>;
ALTER DEFAULT PRIVILEGES FOR ROLE <DB_OWNER> IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO <DB_USER>;

-- 6) 탈취된 기존 계정 정리 — 애플리케이션 전환 확인 후 실행
ALTER ROLE <OLD_ADMIN> NOSUPERUSER;
-- 완전 제거는 의존 객체 확인 후: DROP ROLE <OLD_ADMIN>;
```

## 검증

```sql
-- app 계정에 DDL 권한이 없어야 한다 (ERROR: permission denied 가 정상)
\c bandage <DB_USER>
CREATE TABLE should_fail (id int);

-- owner 는 성공해야 한다
\c bandage <DB_OWNER>
CREATE TABLE ok_to_drop (id int); DROP TABLE ok_to_drop;
```

애플리케이션 기동 시 `ddl-auto: validate` 는 읽기 전용 메타데이터 조회만 하므로
`<DB_USER>` 권한으로 정상 동작한다.
