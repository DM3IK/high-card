package it.sara.demo.web.user;

import it.sara.demo.service.database.FakeDatabase;
import it.sara.demo.service.database.model.User;
import it.sara.demo.web.security.TestTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    private static final String USER_URL = "/user/v1/user";
    private static final String BEARER = "Bearer " + TestTokens.valid("users:read", "users:write");

    @Autowired
    private MockMvc mockMvc;

    private final List<User> seed = List.copyOf(FakeDatabase.TABLE_USER);

    @AfterEach
    void restoreTable() {
        FakeDatabase.TABLE_USER.retainAll(seed);
    }

    @Test
    void putUser_withValidData_storesNormalizedUser() throws Exception {
        String body = """
                {"firstName":" Mario ","lastName":"D'Angelo","email":" mario.dangelo@example.com ",
                 "phoneNumber":"0039 333 123 4567"}
                """;

        mockMvc.perform(put(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200));

        User stored = FakeDatabase.TABLE_USER.get(FakeDatabase.TABLE_USER.size() - 1);
        assertAll(
                () -> assertEquals(seed.size() + 1, FakeDatabase.TABLE_USER.size()),
                () -> assertEquals("Mario", stored.getFirstName()),
                () -> assertEquals("D'Angelo", stored.getLastName()),
                () -> assertEquals("mario.dangelo@example.com", stored.getEmail()),
                () -> assertEquals("+393331234567", stored.getPhoneNumber())
        );
    }

    /**
     * Regression for README task 2: the PUT endpoint stored any text in the name fields,
     * so an SQL injection payload would have reached the storage layer.
     */
    @Test
    void putUser_withSqlInjectionPayload_isRejectedAndNothingIsStored() throws Exception {
        String body = """
                {"firstName":"Robert'); DROP TABLE users;--","lastName":"Rossi",
                 "email":"mario.rossi@example.com","phoneNumber":"+393331234567"}
                """;

        mockMvc.perform(put(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message").value("Invalid first name"));

        assertEquals(seed.size(), FakeDatabase.TABLE_USER.size());
    }

    @Test
    void searchUsers_withEmptyBody_returnsFirstPageSortedByLastName() throws Exception {
        mockMvc.perform(post(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.users.length()").value(10))
                .andExpect(jsonPath("$.users[0].lastName").value("Bianchi"))
                .andExpect(jsonPath("$.users[9].lastName").value("Rossi"));
    }

    @Test
    void searchUsers_withoutBody_returnsFirstPageSortedByLastName() throws Exception {
        mockMvc.perform(post(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.users.length()").value(10))
                .andExpect(jsonPath("$.users[0].lastName").value("Bianchi"))
                .andExpect(jsonPath("$.users[9].lastName").value("Rossi"));
    }

    @Test
    void searchUsers_withQueryOrderAndLimit_returnsSortedPageAndTotalMatches() throws Exception {
        String body = """
                {"query":"RO","order":"BY_FIRSTNAME","offset":0,"limit":2}
                """;

        mockMvc.perform(post(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.users[0].firstName").value("Alessandro"))
                .andExpect(jsonPath("$.users[1].firstName").value("Luca"));
    }

    @Test
    void searchUsers_withInvalidOrder_returns400InvalidOrder() throws Exception {
        mockMvc.perform(post(USER_URL).header(HttpHeaders.AUTHORIZATION, BEARER).contentType(MediaType.APPLICATION_JSON).content("{\"order\":\"BY_AGE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message").value("Invalid order"));
    }
}
