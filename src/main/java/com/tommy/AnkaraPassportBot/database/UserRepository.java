package com.tommy.AnkaraPassportBot.database;

import com.tommy.AnkaraPassportBot.model.UserForAnkara;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserForAnkara, Long> {
}
