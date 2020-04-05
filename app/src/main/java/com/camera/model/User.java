package com.camera.model;

// 单例
public class User{
    private static User user = null;
    private String username;
    private String password;
    private String UID;

    public User(String username,  String password,String UID) {
        this.username = username;
        this.password = password;
        this.UID =UID;
    }

    public String getUsername() {
        return this.username;
    }
    
    public String getPassword() {
        return this.password;
    }

    public String getUID(){
        return this.UID;
    }
}
