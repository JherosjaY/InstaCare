package com.example.instacare;

public class ContactItem {
    private int id;
    private String name;
    private String relation;
    private String number;

    public ContactItem(int id, String name, String relation, String number) {
        this.id = id;
        this.name = name;
        this.relation = relation;
        this.number = number;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getRelation() { return relation; }
    public String getNumber() { return number; }
}
