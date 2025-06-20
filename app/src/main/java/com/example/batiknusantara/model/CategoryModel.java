package com.example.batiknusantara.model;

public class CategoryModel {
    private String name;
    private int iconResource;

    public CategoryModel(String name, int iconResource) {
        this.name = name;
        this.iconResource = iconResource;
    }

    public String getName() {
        return name;
    }

    public int getIconResource() {
        return iconResource;
    }
}