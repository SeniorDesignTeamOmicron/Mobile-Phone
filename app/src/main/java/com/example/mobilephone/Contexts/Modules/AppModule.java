package com.example.mobilephone.Contexts.Modules;

import android.app.Application;

import com.example.mobilephone.Databases.StepSummaryDao;
import com.example.mobilephone.Databases.StepSummaryDatabase;
import com.example.mobilephone.Databases.UserDao;
import com.example.mobilephone.Databases.UserDatabase;
import com.example.mobilephone.Repositories.LocationRepository;
import com.example.mobilephone.Services.LogistepsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import androidx.room.Room;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module(includes = {ViewModelModule.class})
public class AppModule {
    //Databases
    @Provides
    @Singleton
    StepSummaryDatabase provideSummaryDatabase(Application application) {
        return Room.databaseBuilder(application, StepSummaryDatabase.class, "StepSummary.db")
                .build();
    }

    @Provides
    @Singleton
    UserDatabase provideUserDatabase(Application application) {
        return Room.databaseBuilder(application, UserDatabase.class, "User.db")
                .build();
    }

    @Provides
    @Singleton
    StepSummaryDao provideStepSummaryDao(StepSummaryDatabase database) {
        return database.stepSummaryDao();
    }

    @Provides
    @Singleton
    UserDao provideUserDao(UserDatabase database) {
        return database.userDao();
    }

    @Provides
    @Singleton
    LocationRepository provideLocationRepository(Application application) {
        return new LocationRepository(application.getApplicationContext());
    }

    //Repositories
    @Provides
    Executor provideExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    //API
    private static String BASE_URL = "https://senior-design-1549134040092.appspot.com/api/";

    @Provides
    Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides
    Retrofit provideRetrofit(Gson gson) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(BASE_URL)
                .client(createHttpDebuggerClient())
                .build();
        return retrofit;
    }

    @Provides
    @Singleton
    LogistepsService provideApiWebservice(Retrofit restAdapter) {
        return restAdapter.create(LogistepsService.class);
    }

    private OkHttpClient createHttpDebuggerClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        return client;
    }
}
