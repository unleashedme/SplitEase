package com.example.splitease.models;

public enum SplitType {
    EQUAL,       // split evenly between the chosen participants
    EXACT,       // each participant's value is the amount they owe
    PERCENTAGE,  // each participant's value is a percentage of the total; must add up to 100
    SHARES       // each participant's value is a weight, e.g. 2 shares vs 1 share
}
