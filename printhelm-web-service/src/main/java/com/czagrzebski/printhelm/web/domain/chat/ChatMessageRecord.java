package com.czagrzebski.printhelm.web.domain.chat;

public class ChatMessageRecord {

    private String role;
    private String content;

    public ChatMessageRecord() {}

    public ChatMessageRecord(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
