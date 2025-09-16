package com.thalesgroup.tshpaysample.sdk.push;

import java.io.Serializable;

public record ServerMessageInfo(String tokenizedCardId,
                                String messageCode) implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes

    @Override
    public String toString() {
        return "ServerMessageInfo{" +
                "tokenizedCardId='" + tokenizedCardId + '\'' +
                ", messageCode='" + messageCode + '\'' +
                '}';
    }

}
