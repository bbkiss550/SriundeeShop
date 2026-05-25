package com.sriundee.preorder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sriundee.preorder.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findFirstByUsernameIgnoreCaseAndDelete(String username, String delete);
}
