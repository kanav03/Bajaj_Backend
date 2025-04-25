package com.bajaj.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class User {
    private int id;
    private String name;
    private List<Integer> follows;
} 