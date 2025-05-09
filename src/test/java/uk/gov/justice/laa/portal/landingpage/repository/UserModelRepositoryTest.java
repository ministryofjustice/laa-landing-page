package uk.gov.justice.laa.portal.landingpage.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserModelRepositoryTest {

    @Autowired
    private UserModelRepository userModelRepository;

    @Test
    @DisplayName("Test :Save user Test")
    public void saveEmployeeTest() {

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
    public void getListOfUsersTest() {
        //Action
        UserModel user = new UserModel();
        user.setId("123");
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setFullName("John Smith");
        userModelRepository.save(user);
        List<UserModel> users = userModelRepository.findAll();
        //Verify
        assertThat(users.size()).isGreaterThan(0);
    }

}