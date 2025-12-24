package com.example.splitease.repo;

import com.example.splitease.models.Groups;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupRepo extends JpaRepository<Groups, UUID> {

}
