package com.example.demo.exception;

public class DuplicateResourceException extends RuntimeException{
    
    public DuplicateResourceException(String field, String value){
        super(String.format("%s '%s' already exists", field, value));
    }

}
