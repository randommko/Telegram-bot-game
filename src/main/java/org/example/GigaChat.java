package org.example;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;

public class GigaChat {
    public GigaChat(String authKey) {
         GigaChatClient.builder()
                .verifySslCerts(false) // в тестах часто выключают
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS) // или GIGACHAT_API_B2B
                                .authKey(authKey)
                                .build())
                        .build())
                .build();
    }

}
