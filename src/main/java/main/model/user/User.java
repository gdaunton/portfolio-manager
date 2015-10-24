package main.model.user;

import java.io.Serializable;

public class User implements Serializable {
    public String username;
    public String passwordHash;
    private int id;

    /**
     * Creates a new User object
     *
     * @param username the user's username
     * @param hash     the password hash
     * @param id       the user's id
     */
    public User(String username, String hash, int id) {
        this.username = username;
        this.passwordHash = hash;
    }

    /**
     * Get this users ID
     *
     * @return
     */
    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return username;
    }

    @Override
    public boolean equals(Object user) {
        return ((User) user).username.equals(this.username) && ((User) user).passwordHash.equals(this.passwordHash);
    }
}