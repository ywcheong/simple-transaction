-- 애플리케이션에 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE ON stdb.* TO 'stserver'@'%';

-- 권한 발행
FLUSH PRIVILEGES;