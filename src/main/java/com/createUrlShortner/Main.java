package com.createUrlShortner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper(); //Precisamos add de uma biblioteca chamada Jatson

    private final S3Client s3Client = S3Client.builder().build();

    @Override //HandleRequest e um metodo para extrair infos que o usuario vai informar na requisição: URL e tempo de
    // expiração
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString(); //Criando variável para extrair o body da requisição e Fazendo um
        // casting e informando que o input do usuario é uma string

        //Desserializar o body para obter os dados que o usuário informou
        //Antes usar o try-catch porque não sabemos se todas as informações serão enviadas corretamente

        Map<String, String> bodyMap;
        try {
            bodyMap = objectMapper.readValue(body, Map.class); //Para transformar o body em Map, mas tem que add uma
            // dependencia
        } catch (Exception exception) {
                throw new RuntimeException("Error parsing JSON body: " + exception.getMessage(), exception);
        }

        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");

        long expirationTimeInSeconds = Long.parseLong(expirationTime);

        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        UrlData urlData = new UrlData(originalUrl, expirationTimeInSeconds);

        try {
            String urlDataJson = objectMapper.writeValueAsString(urlData);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("url-shortener-storage-project-rocketseat-luiza-costa")
                    .key(shortUrlCode + ".json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(urlDataJson));
        } catch (Exception exception){
            throw new RuntimeException("Error saving data to S3: " + exception.getMessage(), exception);
        }

        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);

        return response;

    }
}