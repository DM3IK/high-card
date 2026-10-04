package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory replacement for a database, seeded with sample users at startup.
 */
public class FakeDatabase {

    /** User table; thread-safe because concurrent requests can read and write it. */
    public static final List<User> TABLE_USER = new CopyOnWriteArrayList<>();

    static {
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setGuid(UUID.randomUUID().toString());
            user.setFirstName("First name " + i);
            user.setLastName("Last name " + i);
            user.setEmail("user" + i + "@example.com");
            user.setPhoneNumber("+39" + i);
            TABLE_USER.add(user);
        }
    }

    private FakeDatabase() {

    }

}
