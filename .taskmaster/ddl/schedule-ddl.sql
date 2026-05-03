-- =====================================================================
-- Schedule (BD-16) — DDL 참조 문서
--
-- 본 스크립트는 현재 엔티티 매핑(JPA / Hibernate) 기준으로 PostgreSQL
-- 에서 생성되는 실제 스키마를 정리한 참조 문서다.
--
-- 운영상 DDL 적용 방식:
--   - local 프로필:    application-default-datasource.yaml 의 ddl-auto: create
--                       (부팅 시 Hibernate 가 자동 생성/재생성)
--   - test 프로필:     application-test.yaml 의 ddl-auto: create-drop (H2 PostgreSQL 모드)
--   - dev/prod 프로필: application-dev-datasource.yaml 의 ddl-auto: validate
--                       (스키마 변경은 별도 마이그레이션 절차 필요)
--
-- PRD 초안과 차이점:
--   1. 패키지/도메인명 schedule_coordination → schedule (사용자 결정)
--   2. JSONB 컬럼 미사용. availableDates / unavailableDates / blocks 는
--      JPA @ElementCollection + @CollectionTable 패턴으로 별도 테이블 분리
--      (프로젝트 전체 컨벤션 일치)
--   3. 모든 테이블에 `p_` prefix (프로젝트 컨벤션)
--   4. BaseEntity 상속으로 audit 컬럼 일관 적용
--      (created_at, last_modified_at, deleted_at, created_by, last_modified_by, deleted_by)
--   5. ScheduleBlock 의 PK 는 client-supplied UUIDv7 (PUT 의 upsert 의미 지원)
-- =====================================================================

-- ---------------------------------------------------------------------
-- p_member_schedule (멤버 가용 시간 헤더)
-- ---------------------------------------------------------------------
CREATE TABLE p_member_schedule (
    meeting_id              UUID                NOT NULL,
    user_id                 BIGINT              NOT NULL,
    note                    VARCHAR(500),
    completed               BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP           NOT NULL DEFAULT NOW(),
    last_modified_at        TIMESTAMP           NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMP,
    created_by              BIGINT,
    last_modified_by        BIGINT,
    deleted_by              BIGINT,
    PRIMARY KEY (meeting_id, user_id),
    FOREIGN KEY (meeting_id) REFERENCES p_setlist_meeting(meeting_id)
);

-- p_member_schedule_available_dates (가용 일자 collection)
CREATE TABLE p_member_schedule_available_dates (
    meeting_id              UUID                NOT NULL,
    user_id                 BIGINT              NOT NULL,
    date_value              DATE                NOT NULL,
    FOREIGN KEY (meeting_id, user_id)
        REFERENCES p_member_schedule(meeting_id, user_id)
        ON DELETE CASCADE
);
CREATE INDEX idx_pmsa_owner ON p_member_schedule_available_dates(meeting_id, user_id);

-- p_member_schedule_unavailable_dates
CREATE TABLE p_member_schedule_unavailable_dates (
    meeting_id              UUID                NOT NULL,
    user_id                 BIGINT              NOT NULL,
    date_value              DATE                NOT NULL,
    FOREIGN KEY (meeting_id, user_id)
        REFERENCES p_member_schedule(meeting_id, user_id)
        ON DELETE CASCADE
);
CREATE INDEX idx_pmsu_owner ON p_member_schedule_unavailable_dates(meeting_id, user_id);

-- p_member_schedule_blocks (Map<LocalDate, "12-hex">)
CREATE TABLE p_member_schedule_blocks (
    meeting_id              UUID                NOT NULL,
    user_id                 BIGINT              NOT NULL,
    date_key                DATE                NOT NULL,
    block_hex               VARCHAR(12)         NOT NULL,
    PRIMARY KEY (meeting_id, user_id, date_key),
    FOREIGN KEY (meeting_id, user_id)
        REFERENCES p_member_schedule(meeting_id, user_id)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- p_schedule_board (시간표 시안)
-- ---------------------------------------------------------------------
CREATE TABLE p_schedule_board (
    schedule_board_id        UUID                PRIMARY KEY,        -- UUIDv7 (Hibernate @UuidGenerator)
    meeting_id               UUID                NOT NULL,
    name                     VARCHAR(50)         NOT NULL,
    palette_seed             INT,
    confirmed                BOOLEAN             NOT NULL DEFAULT FALSE,
    working_hours_start      INT                 NOT NULL DEFAULT 18,
    working_hours_end        INT                 NOT NULL DEFAULT 44,
    exclude_late_night       BOOLEAN             NOT NULL DEFAULT TRUE,
    max_consecutive_minutes  INT                 NOT NULL DEFAULT 240,
    version                  BIGINT              NOT NULL DEFAULT 0, -- @Version optimistic lock
    created_at               TIMESTAMP           NOT NULL DEFAULT NOW(),
    last_modified_at         TIMESTAMP           NOT NULL DEFAULT NOW(),
    deleted_at               TIMESTAMP,
    created_by               BIGINT,
    last_modified_by         BIGINT,
    deleted_by               BIGINT,
    FOREIGN KEY (meeting_id) REFERENCES p_setlist_meeting(meeting_id)
);
CREATE INDEX idx_p_schedule_board_meeting ON p_schedule_board(meeting_id);

-- ---------------------------------------------------------------------
-- p_schedule_block (시간표 블록)
-- ---------------------------------------------------------------------
CREATE TABLE p_schedule_block (
    schedule_block_id        UUID                PRIMARY KEY,        -- client-supplied UUIDv7 (PUT upsert)
    schedule_board_id        UUID                NOT NULL,
    song_id                  UUID                NOT NULL,           -- p_setlist_item.setlist_item_id 참조 (FK 미설정: cross-domain 느슨한 결합)
    block_date               DATE                NOT NULL,
    start_slot               INT                 NOT NULL,
    duration_slots           INT                 NOT NULL,
    pinned                   BOOLEAN             NOT NULL DEFAULT FALSE,
    palette_index            INT,
    song_title_override      VARCHAR(255),
    note                     VARCHAR(200),
    created_at               TIMESTAMP           NOT NULL DEFAULT NOW(),
    last_modified_at         TIMESTAMP           NOT NULL DEFAULT NOW(),
    deleted_at               TIMESTAMP,
    created_by               BIGINT,
    last_modified_by         BIGINT,
    deleted_by               BIGINT,
    FOREIGN KEY (schedule_board_id)
        REFERENCES p_schedule_board(schedule_board_id)
        ON DELETE CASCADE
);
CREATE INDEX idx_p_schedule_block_board_date ON p_schedule_block(schedule_board_id, block_date);
