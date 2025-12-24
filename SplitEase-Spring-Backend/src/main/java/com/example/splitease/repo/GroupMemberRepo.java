package com.example.splitease.repo;

import com.example.splitease.models.GroupMembers;
import com.example.splitease.models.Groups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMembers, UUID> {
    long countByGroup(Groups group);

    @Query("""
        SELECT gm
        FROM GroupMembers gm
        JOIN FETCH gm.group
        WHERE gm.user.email = :email
    """)
    List<GroupMembers> findAllByUserEmail(String email);

    List<GroupMembers> findByGroupId(UUID groupId);

    List<GroupMembers> findByUserEmail(String email);
}
