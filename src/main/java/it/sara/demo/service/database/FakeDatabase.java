package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory replacement for a database, seeded with sample users at startup.
 */
public class FakeDatabase {

    /** User table; thread-safe because concurrent requests can read and write it. */
    public static final List<User> TABLE_USER = new CopyOnWriteArrayList<>();

    private static final String[][] SEED_NAMES = {
            {"Mario", "Rossi"},
            {"Giulia", "Bianchi"},
            {"Luca", "Romano"},
            {"Francesca", "Colombo"},
            {"Marco", "Ricci"},
            {"Chiara", "Marino"},
            {"Alessandro", "Greco"},
            {"Sara", "Bruno"},
            {"Davide", "Gallo"},
            {"Elena", "Conti"}
    };

    static {
        for (int i = 0; i < SEED_NAMES.length; i++) {
            String firstName = SEED_NAMES[i][0];
            String lastName = SEED_NAMES[i][1];
            User user = new User();
            user.setGuid(UUID.randomUUID().toString());
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail((firstName + "." + lastName).toLowerCase(Locale.ROOT) + "@example.com");
            user.setPhoneNumber("+39333000000" + i);
            TABLE_USER.add(user);
        }
    }

    private FakeDatabase() {

    }

}
