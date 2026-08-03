#!/bin/bash
# postgres 컨테이너 최초 기동(빈 데이터 디렉토리) 시 1회 실행된다.
#   - owner (DB_OWNER) : 스키마 소유자. Liquibase 마이그레이션(DDL) 전용
#   - app   (DB_APP)   : 런타임 전용. DML 만 가능하고 DDL 권한 없음
# 슈퍼유저(POSTGRES_USER)는 관리 목적으로만 쓰고 애플리케이션은 사용하지 않는다.
#
# 주의: 이미 데이터가 있는 볼륨에서는 실행되지 않는다. 기존 DB 에는
#       docs/migration/db-role-separation.md 의 수동 절차를 적용할 것.
set -euo pipefail

# ponytail: 역할 생성만 gexec 로 조건 분기. 나머지는 멱등한 DDL 이라 그대로 실행한다.
psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v owner="$DB_OWNER" -v owner_pw="$DB_OWNER_PASSWORD" \
  -v app="$DB_APP" -v app_pw="$DB_APP_PASSWORD" \
  -v db="$POSTGRES_DB" <<'EOSQL'
-- 역할 생성 (재실행 안전). 없을 때만 CREATE ROLE 문을 생성해 실행한다.
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'owner', :'owner_pw')
 WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'owner');
\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app', :'app_pw')
 WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'app');
\gexec

-- public 스키마 소유권을 owner 로 이전하고, 기본 CREATE 권한을 회수한다.
ALTER SCHEMA public OWNER TO :"owner";
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- app 은 연결/DML 만. DDL(CREATE) 권한은 주지 않는다.
GRANT CONNECT ON DATABASE :"db" TO :"app";
GRANT USAGE ON SCHEMA public TO :"app";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"app";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app";

-- owner 가 앞으로 만드는 객체에도 동일 권한이 자동 적용되도록 기본 권한을 설정.
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app";
EOSQL
