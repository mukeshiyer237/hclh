package com.example.demo.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLifecycleEventRepository extends JpaRepository<UserLifecycleEvent, Long> {

    List<UserLifecycleEvent> findByUserId(Long userId);

    List<UserLifecycleEvent> findByEventType(String eventType);
}
