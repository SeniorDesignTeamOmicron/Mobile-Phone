package com.example.mobilephone.Contexts.Modules;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobilephone.ViewModels.ShoeViewModel;
import com.example.mobilephone.Contexts.keys.ViewModelKey;
import com.example.mobilephone.ViewModels.FactoryViewModel;
import com.example.mobilephone.ViewModels.StepSummaryViewModel;
import com.example.mobilephone.ViewModels.UserViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(StepSummaryViewModel.class)
    abstract ViewModel bindStepSummaryViewModel(StepSummaryViewModel stepSummaryViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel.class)
    abstract ViewModel bindUserViewModel (UserViewModel userViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ShoeViewModel.class)
    abstract ViewModel bindShoeViewModel (ShoeViewModel shoeViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(FactoryViewModel factory);
}
