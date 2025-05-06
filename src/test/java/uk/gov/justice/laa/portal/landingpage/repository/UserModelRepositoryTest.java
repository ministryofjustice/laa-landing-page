package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserModelRepositoryTest {

    @Autowired
    private UserModelRepository userModelRepository;

    @Test
    @DisplayName("Test 1:Save user Test")
    @Order(1)
    @Rollback(value = false)
    public void saveEmployeeTest(){

        //Action
        UserModel user = new UserModel();
        user.setId("123");
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setFullName("John Smith");
        userModelRepository.save(user);

        //Verify
        assertThat(user.getUid()).isNotNull();
    }

    @Test
    @Order(2)
    public void getListOfUsersTest(){
        //Action
        List<UserModel> users = userModelRepository.findAll();
        //Verify
        assertThat(users.size()).isGreaterThan(0);
    }

}