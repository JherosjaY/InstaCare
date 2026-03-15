package com.example.instacare;

public class ContactItem {
    private String name;
    private String relation;
    private String number;

    public ContactItem(String name, String relation, String number) {
        this.name = name;
        this.relation = relation;
        this.number = number;
    }

    public String getName() { return name; }
    public String getRelation() { return relation; }
    public String getNumber() { return number; }
}
