package az.code.turaltelegrambot.redis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RedisDataRepo extends JpaRepository<RedisEntity, Long> {

}
