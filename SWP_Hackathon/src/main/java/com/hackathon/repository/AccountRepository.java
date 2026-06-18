package com.hackathon.repository;

import com.hackathon.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD

=======
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Component;
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1

@Component
public interface AccountRepository extends JpaRepository<Account, Integer> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    java.util.Optional<Account> findByEmail(String email);

    java.util.Optional<Account> findByVerificationToken(String verificationToken);

//    boolean exitsByStudent_StudentIdAndLea

}
