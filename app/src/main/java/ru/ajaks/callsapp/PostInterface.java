package ru.ajaks.callsapp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface PostInterface {
    @POST
    Call<String> createPost(@Url String url, @Body String xml);
}
