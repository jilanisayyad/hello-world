package com.example;

public class Main {
    public static void main(String[] args) {
        while (true) {
            System.out.println("Hello, World!");
            try {
                Thread.sleep(2000); // Sleep for 2000 milliseconds (2 seconds)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}