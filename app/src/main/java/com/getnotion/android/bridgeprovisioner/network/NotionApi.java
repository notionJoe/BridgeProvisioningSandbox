/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.network;


import com.getnotion.android.bridgeprovisioner.models.NotionBridge;
import com.getnotion.android.bridgeprovisioner.models.NotionSystem;
import com.getnotion.android.bridgeprovisioner.models.NotionUser;
import com.getnotion.android.bridgeprovisioner.models.wrappers.BridgePostResponse;
import com.getnotion.android.bridgeprovisioner.models.wrappers.BridgeRequest;
import com.getnotion.android.bridgeprovisioner.models.wrappers.BridgeResponse;
import com.getnotion.android.bridgeprovisioner.models.wrappers.LoginRequest;

import retrofit.Callback;
import retrofit.ResponseCallback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

public interface NotionApi {

    //////////////////////////////////////////////////////////////////////////////
    // User API

    // Get the user specified by :id
    @GET("/api/users/{id}")
    void getUser(@Path("id") int user,
                 Callback<NotionUser.UserResponse> cb);

    // Update the user specified by :id
    @PUT("/api/users/{id}")
    void updateUser(@Path("id") int userId,
                    @Body NotionUser.UserRequest userRequest,
                    Callback<NotionUser.UserResponse> cb);

    // Sign up a new user with the given email and password
    @POST("/api/users")
    void createUser(@Body NotionUser.UserRequest user,
                    Callback<NotionUser.UserResponse> cb);

    // Delete the user specified by :id
    @DELETE("/api/users/{id}")
    void deleteUser(@Path("id") int user,
                    ResponseCallback cb);
    //////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////
    // Session API
    // Sign in
    @POST("/api/users/sign_in")
    void login(@Body LoginRequest loginRequest,
               @Query("systems") String returnSystem,
               Callback<NotionUser.UserResponse> cb);

    // Sign out
    @DELETE("/api/users/sign_out")
    void logout(ResponseCallback cb);
    //////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////
    // BaseStation API
    @GET("/api/base_stations")
    void getBridges(Callback<BridgeResponse> cb);

    @GET("/api/base_stations/{id}")
    NotionBridge getBridge(@Path("id") int bridgeId);

    @POST("/api/base_stations")
    void createBridge(@Body BridgeRequest baseStationRequest,
                      Callback<BridgePostResponse> cb);

    @POST("/api/base_stations/{id}/reset")
    void resetBridge(Callback<Response> cb);

    @PUT("/api/base_stations/{id}")
    NotionBridge updateBridge(@Path("id") int baseStationId);

    @DELETE("/api/base_stations/{id}")
    void deleteBridge(@Path("id") int baseStationId,
                      Callback<Response> cb);
    //////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////
    // System API
    @GET("/api/systems")
    void getSystems(Callback<NotionSystem.SystemResponse> cb);
    void getSystems(@Query("page") Integer page,
                    @Query("sensors") Boolean sensors,
                    @Query("locations") Boolean locations,
                    Callback<NotionSystem.SystemResponse> cb);

    // Get the system specified by :id
    @GET("/api/systems/{id}")
    void getSystem(@Path("id") int system,
                   Callback<NotionSystem.SystemGETResponse> cb);

    // Update system specified by :id
    @PUT("/api/systems/{id}")
    void updateSystem(@Path("id") int system,
                      @Body NotionSystem.SystemRequest systemRequest,
                      Callback<NotionSystem.SystemPUTResponse> cb);

    // Update system lat/long specified by :id
    @PUT("/api/systems/{id}")
    void updateSystemLatLong(@Path("id") int system,
                             @Body NotionSystem.SystemLocationRequest systemLocationRequest,
                             Callback<NotionSystem.SystemPUTResponse> cb);

    // Create new system
    @POST("/api/systems")
    void createSystem(@Body NotionSystem.SystemRequest system,
                      Callback<NotionSystem.SystemPOSTResponse> cb);

    // Delete the system specified by :id
    @DELETE("/api/systems/{id}")
    void deleteSystem(@Path("id") int system,
                      Callback<NotionSystem.SystemResponse> cb);
    //////////////////////////////////////////////////////////////////////////////
}
